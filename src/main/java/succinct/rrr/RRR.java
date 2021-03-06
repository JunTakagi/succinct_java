package succinct.rrr;

import java.util.Map;
import java.util.Arrays;

import java.lang.StringBuilder;
import java.math.BigInteger;

import succinct.SuccinctBitVector;
import succinct.util.ConstBlockBitVector;
import succinct.util.BitVectorN;
import succinct.util.BitVector64;
import succinct.util.BitBlockFactory;
import succinct.util.ArbitraryLengthBitVector;

public class RRR implements SuccinctBitVector {
  public static final String CONF_SUPERBLOCK_STEP = "succinct.rrr.superblock.steps";
  public static final String CONF_BLOCK_SIZE = "succinct.rrr.block.size";
  public static final String CONF_R_SAMPLE_STEP = "succinct.rrr.R.sampling";
  public static final double LOG2 = Math.log(2.0);
  public static final long LONGMASK = -1L;

  protected ConstBlockBitVector superBlockRanks;
  protected BitVectorN ks;
  protected ArbitraryLengthBitVector rs;
  protected byte[] rLengthTable;
  protected long[] cmbTable;
  protected int[] rLengthSampling;

  protected int superBlockStep;
  protected int superBlockNum;
  protected int blockSize;
  protected int rSampleStep;

  @Override
  public void build(ConstBlockBitVector bv, Map config) {
    superBlockStep = 0;
    long bitlength = (long)bv.getBlockNum() * (long)bv.getBlockSize();
    int maxRankBits = (int)Math.ceil(Math.log(bitlength) / LOG2);
    int blockNum = 0;

    if (config.containsKey(CONF_SUPERBLOCK_STEP)) {
      superBlockStep = ((Number)(config.get(CONF_SUPERBLOCK_STEP))).intValue();
    } else {
      superBlockStep = (int) Math.floor(Math.log(bitlength));
    }
    if (config.containsKey(CONF_BLOCK_SIZE)) {
      blockSize = ((Number)(config.get(CONF_BLOCK_SIZE))).intValue(); // 7, 15, 31, 63
      if (blockSize > 62) blockSize = 62;
    } else {
      blockSize = 62;
    }

    BitVectorN workingVector = new BitVectorN(blockSize);
    workingVector.copy((BitVector64) bv);

    blockNum = (int)(((long)bv.getBlockSize() * (long)bv.getBlockNum() - 1L) / blockSize) + 1;

    // prepare super block rank sampling
    superBlockRanks = BitBlockFactory.getUpTo(maxRankBits);

    superBlockNum = ((blockNum-1) / superBlockStep) + 2;

    superBlockRanks.constructAsIndex(superBlockNum);

    // prepare K vector
    int kSize = (int)Math.ceil(Math.log(blockSize+1)/LOG2);
    ks = new BitVectorN(kSize);
    ks.construct(blockNum);

    // prepare R size
    rLengthTable = new byte[blockSize+1];
    BigInteger cmb4len = BigInteger.valueOf(1L);
    rLengthTable[0] = 0;
    for (int k=1; k<blockSize+1; k++) {
      cmb4len = cmb4len.multiply(BigInteger.valueOf(blockSize-k+1)).divide(BigInteger.valueOf(k));
      rLengthTable[k] = (byte) Math.ceil(Math.log(cmb4len.longValue())/LOG2);
    }

    // calc cmb(t-1,k) table
    cmbTable = new long[blockSize];
    long cmb = 1L;
    for (int k=1; k<blockSize; k++) {
      cmb = (cmb*(blockSize-k))/k;
      cmbTable[k] = cmb;
    }

    // prepare partial sum support region
    if (config.containsKey(CONF_R_SAMPLE_STEP)) {
      rSampleStep = ((Number)(config.get(CONF_R_SAMPLE_STEP))).intValue();
    } else {
      rSampleStep = (int) Math.floor(Math.log(blockNum));
    }
    int rLengthSuperBlockNum = (blockNum-1) / rSampleStep + 1;
    rLengthSampling = new int[rLengthSuperBlockNum];
    Arrays.fill(rLengthSampling, 0);

    // construct ks and r size
    long totalRank = 0L;
    int totalRLength = 0;
    for (int i=0; i<blockNum; i++) {
      if (i % superBlockStep == 0) {
        superBlockRanks.setBlock(i / superBlockStep, totalRank);
      }
      if (i % rSampleStep == 0) {
        rLengthSampling[i / rSampleStep] = totalRLength;
      }
      long block = workingVector.access(i);
      int klass = popcount(block);
      int curRLength = rLengthTable[klass];

      ks.setNext(klass);

      totalRank += klass;
      totalRLength += curRLength;
    }
    superBlockRanks.setBlock(superBlockNum-1, totalRank);

    rs = new ArbitraryLengthBitVector();
    rs.construct(totalRLength);
    for (int i=0; i<blockNum; i++) {
      long block = workingVector.access(i);
      int klass = (int)ks.access(i);
      writeR(blockSize, klass, block);
    }
  }

  protected void writeR(int blockSize, int k, long block) {
    if (k == blockSize) return;
    if (k == 0) return;

    long r = calcR(k, block);

    int rLength = rLengthTable[k];

    for (int i=0; i<rLength; i++) {
      rs.setNext((int)((r >>> i) & 1L));
    }
  }

  public byte getRLength(int k) {
    return rLengthTable[k];
  }

  public long calcR(int k, long block) {
    if (k == blockSize) return 0;
    if (k == 0) return 0;

    long r = 0L;
    int t = blockSize;

    long cmb = cmbTable[k];

    for (int i=blockSize-1; i>=0; i--) {
      int check = (int)((block >>> i) & 1L);

      if (check == 1) {
        r += cmb;
        k--;
        t--;

        if (k == 0) break;

        // next cmb
        cmb = (cmb*(k+1))/t; // (t'-1-1)C(k'-1). t', k' is previous t and k.
      } else {
        t--;

        if (t == k) break;

        cmb = (cmb*(t-k))/t; // (t'-1-1)C(k). t' is previous t.
      }
    }
    return r;
  }

  public long decodeR(int klass, long r) {
    long block = 0L;

    if (klass == 0) return 0L;
    if (blockSize == klass) {
      return LONGMASK >>> (64 - blockSize);
    }

    int t = blockSize;
    int k=klass;

    long cmb = cmbTable[k];

    for (int i=0; i<blockSize; i++) {
      block <<= 1;

      if (r >= cmb) {
        block |= 1L;
        r -= cmb;
        t--;
        k--;

        if (r == 0) { // rest ones are alined at most least bits side.
          int restbits = blockSize - 1 - i;
          block <<= restbits;
          if (k != 0) { // at least 1 ones left.
            block |= LONGMASK >>> (64 - k);
          }
          break;
        }

        cmb = (cmb*(k+1))/t; // (t'-1-1)C(k'-1). t', k' is previous t and k.
      } else {
        t--;

        cmb = (cmb*(t-k))/t; // (t'-1-1)C(k). t' is previous t.
      }
    }
    return block;
  }

  public long getR(int pos) {
    int rSampleBlock = pos / rSampleStep;
    int rStartPos = rLengthSampling[rSampleBlock];

    for (int i=rSampleBlock*rSampleStep; i<pos; i++) {
      rStartPos += rLengthTable[(int)ks.access(i)];
    }

    int rEndPos = rStartPos + rLengthTable[(int)ks.access(pos)];
    if (rStartPos == rEndPos) return 0;
    long r = rs.access(rStartPos, rEndPos-1);
    return r;
  }

  @Override
  public long rank(long bitpos) {
    int blockPos = (int)(bitpos / blockSize);
    int innerBlockPos = (int)(bitpos % blockSize);
    int superBlockPos = blockPos / superBlockStep;

    long ret = superBlockRanks.access(superBlockPos);

    for (int i=superBlockPos*superBlockStep; i<blockPos; i++) {
      ret += ks.access(i);
    }
    if (innerBlockPos == blockSize) {
      return ret + ks.access(blockPos);
    }

    int klass = (int)ks.access(blockPos);
    long blockContents = 0L;
    if (klass == 0) {
      return ret;
    } else if (klass == blockPos) {
      blockContents = decodeR(klass, 0L);
    } else {
      long r = getR(blockPos); // $B$3$3$N%3%9%H=E$$$N$G$5$\$l$k$J$i$5$\$k(B
      blockContents = decodeR(klass, r);
    }

    blockContents &= ~(LONGMASK << (innerBlockPos+1));

    ret += popcount(blockContents);

    return ret;
  }

  @Override
  public long select(long num) {
    int superblock = (int)binarySearch(num, 0, superBlockNum);
    int blockStart = superblock * superBlockStep;
    int blockEnd = blockStart + superBlockStep;

    long prevrank = superBlockRanks.access(superblock);
    long totalrank = prevrank;

    for (int i=blockStart; i<blockEnd; i++) {
      int klass = (int)ks.access(i);
      totalrank += klass;
      if (totalrank >= num) {
        long blockContents = 0L;
        if (klass == blockSize) {
          blockContents = decodeR(klass, 0L);
        } else {
          long r = getR(i);
          blockContents = decodeR(klass, r);
        }

        long innerBlockPos = sequentialSearch(blockContents, (int)(num-prevrank), blockSize);
        return ((long)i) * blockSize + innerBlockPos;
      }
      prevrank = totalrank;
    }
    return -1L; // $B$3$3$XMh$k$3$H$OK\Mh$J$$(B
  }

  protected long binarySearch(long num, int start, int end) {
    if (end <= start + 1) return start;

    int middle = (start + end)/2;
    if (num <= superBlockRanks.access(middle)) {
      return binarySearch(num, start, middle);
    }
    return binarySearch(num, middle, end);
  }

  static public int sequentialSearch(long block, int num, int blockSize) {
    for (int i=0; i<blockSize; i++) {
      if (((block >> i) & 1L) == 1L) {
        num--;
        if (num <= 0L) return i;
      }
    }
    return -1; // $B$3$3$X$OK\MhMh$J$$(B
  }

  static public int popcount(long r) {
    r = (r & 0x5555555555555555L) + ((r & 0xaaaaaaaaaaaaaaaaL) >>> 1);
    r = (r & 0x3333333333333333L) + ((r & 0xccccccccccccccccL) >>> 2);
    r = (r & 0x0f0f0f0f0f0f0f0fL) + ((r & 0xf0f0f0f0f0f0f0f0L) >>> 4);
    r = (r & 0x00ff00ff00ff00ffL) + ((r & 0xff00ff00ff00ff00L) >>> 8);
    r = (r & 0x0000ffff0000ffffL) + ((r & 0xffff0000ffff0000L) >>> 16);
    r = (r & 0x00000000ffffffffL) + ((r & 0xffffffff00000000L) >>> 32);
    return (int) r;
  }
  @Override
  public long size() {
    long ret = 0L;
    ret += superBlockRanks.size();
    ret += ks.size();
    ret += rs.size();
    ret += 8L * rLengthTable.length;
    ret += 32L * rLengthSampling.length;
    ret += 64L * cmbTable.length;
    return ret;
  }
  @Override
  public String sizeInfo() {
    StringBuilder info = new StringBuilder("");
    info.append("super block rank regeion\t").append(String.valueOf(superBlockRanks.size()));
    info.append("\nblock class regeion\t").append(String.valueOf(ks.size()));
    info.append("\nblock r index regeion\t").append(String.valueOf(rs.size()));
    info.append("\nr index length table regeion\t").append(String.valueOf(8L * rLengthTable.length));
    info.append("\nr length partial sum regeion\t").append(String.valueOf(32L *  rLengthSampling.length));
    info.append("\nr cmb table regeion\t").append(String.valueOf(64L * cmbTable.length));
    return info.toString();
  }
}
