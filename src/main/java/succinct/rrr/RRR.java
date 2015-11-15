package succinct.rrr;

import java.util.Map;
import java.util.Arrays;

import org.apache.commons.math3.util.CombinatoricsUtils;

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
  protected BitVectorN rLength;
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
    } else {
      blockSize = 63;
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
    int rMaxSize = (int)Math.ceil(Math.log(CombinatoricsUtils.binomialCoefficient(blockSize, blockSize/2)) / LOG2);
    rLength = new BitVectorN(rMaxSize);

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
      int curRLength = (int)Math.ceil(Math.log(CombinatoricsUtils.binomialCoefficient(blockSize, klass)) / LOG2);

      ks.setNext(klass);
      rLength.setNext(curRLength);

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

    int t = blockSize;

    long cmb = CombinatoricsUtils.binomialCoefficient(t-1, k);
    int r = 0;
    for (int i=blockSize-1; i>=0; i++) {
      int check = (int)((block >> i) & 1L);
      if (check == 1) {
        r += cmb;

        // next cmb
        cmb = (cmb / (t-1)) * k;

        k -= 1;
      } else {
        cmb = (cmb / (t-1)) * (t-1-k);
      }
      t -= 1;
      if (k == t) break;
    }

    int rLength = (int)Math.ceil(Math.log(CombinatoricsUtils.binomialCoefficient(blockSize, k)) / LOG2);
    for (int i=0; i<rLength; i++) {
      rs.setNext((r >>> i) & 1);
    }
  }

  protected long decodeR(int blockSize, int klass, int r) {
    long block = 0L;

    if (blockSize == klass) {
      return LONGMASK >>> (64 - blockSize);
    }
    int t = blockSize;
    int k=klass;

    long cmb = CombinatoricsUtils.binomialCoefficient(t-1, k);

    for (int i=0; i<blockSize; i++) {
      block <<= 1;
      if (r >= cmb) {
        block |= 1L;
        r -= cmb;
        if (r == 0) {
          int restbits = blockSize - 1 - i;
          block <<= restbits;
          if (k != 0) { // k == restbits
            block |= LONGMASK >> (64 - restbits);
          }
          break;
        }

        cmb = (cmb / (t-1)) * k;
        k -= 1;
      } else {
        cmb = (cmb / (t-1)) * (t-1-k);
      }
      t -= 1;
    }
    return block;
  }

  @Override
  public long rank(long bitpos) {
    return -1L;
  }

  @Override
  public long select(long num) {
    return -1L;
  }

  private int popcount(long r) {
    r = (r & 0x5555555555555555L) + ((r & 0xaaaaaaaaaaaaaaaaL) >>> 1);
    r = (r & 0x3333333333333333L) + ((r & 0xccccccccccccccccL) >>> 2);
    r = (r & 0x0f0f0f0f0f0f0f0fL) + ((r & 0xf0f0f0f0f0f0f0f0L) >>> 4);
    r = (r & 0x00ff00ff00ff00ffL) + ((r & 0xff00ff00ff00ff00L) >>> 8);
    r = (r & 0x0000ffff0000ffffL) + ((r & 0xffff0000ffff0000L) >>> 16);
    r = (r & 0x00000000ffffffffL) + ((r & 0xffffffff00000000L) >>> 32);
    return (int) r;
  }
}
