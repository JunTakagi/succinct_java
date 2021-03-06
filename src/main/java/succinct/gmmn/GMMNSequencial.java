package succinct.gmmn;

import java.util.Map;

import java.lang.StringBuilder;

import succinct.SuccinctBitVector;
import succinct.util.ConstBlockBitVector;
import succinct.util.BitBlockFactory;

public class GMMNSequencial implements SuccinctBitVector {
  public static final String CONF_SUPERBLOCK_STEP = "succinct.gmmn.superblock.steps";
  public static final double LOG2 = Math.log(2.0);

  protected int superBlockStep;
  protected int superBlockNum;
  protected ConstBlockBitVector superBlockRanks;
  protected ConstBlockBitVector blocks;

  @Override
  public void build(ConstBlockBitVector bv, Map config) {
    superBlockStep = 0;
    long bitlength = (long)bv.getBlockNum() * (long)bv.getBlockSize();
    int maxRankBits = (int)Math.ceil(Math.log(bitlength) / LOG2);

    if (config.containsKey(CONF_SUPERBLOCK_STEP)) {
      superBlockStep = ((Number)(config.get(CONF_SUPERBLOCK_STEP))).intValue();
    } else {
      superBlockStep = (int) Math.floor(Math.log(bitlength));
    }

    superBlockRanks = BitBlockFactory.getUpTo(maxRankBits);

    superBlockNum = ((bv.getBlockNum()-1) / superBlockStep) + 2;

    superBlockRanks.constructAsIndex(superBlockNum);

    long totalRank = 0L;
    for (int i=0; i<bv.getBlockNum(); i++) {
      if (i % superBlockStep == 0) {
        superBlockRanks.setBlock(i / superBlockStep, totalRank);
      }
      totalRank += bv.popcount(i);
    }
    superBlockRanks.setBlock(superBlockNum-1, totalRank);

    blocks = bv;
  }

  @Override
  public long rank(long bitpos) {
    int innerBlockPos = (int) (bitpos % blocks.getBlockSize());
    int block = (int)(bitpos / blocks.getBlockSize());
    int superblock = block / superBlockStep;
    int blockStart = superBlockStep * superblock;

    long ret = superBlockRanks.access(superblock);
    for (int i=blockStart; i<block; i++) {
      ret += blocks.popcount(i);
    }
    if (innerBlockPos == blocks.getBlockSize()-1) {
      return ret + blocks.popcount(block);
    }
    return ret + blocks.popcount(block, innerBlockPos);
  }

  @Override
  public long select(long num) {
    int superblock = (int)binarySearch(num, 0, superBlockNum);
    int blockStart = superblock * superBlockStep;
    int blockEnd = blockStart + superBlockStep;

    long prevrank = superBlockRanks.access(superblock);
    long totalrank = prevrank;

    for (int i=blockStart; i<blockEnd; i++) {
      totalrank += blocks.popcount(i);
      if (totalrank >= num) {
        long innerBlockPos = blocks.select(i, (int)(num-prevrank));
        return ((long)i) * blocks.getBlockSize() + innerBlockPos;
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

  @Override
  public long size() {
    long ret = 0L;
    ret += superBlockRanks.size();
    ret += blocks.size();
    return ret;
  }
  @Override
  public String sizeInfo() {
    StringBuilder info = new StringBuilder();
    info.append("original block regeion:\t").append(String.valueOf(blocks.size()));
    info.append("\nsuperblock rank regeion:\t").append(String.valueOf(superBlockRanks.size()));
    return info.toString();
  }
}
