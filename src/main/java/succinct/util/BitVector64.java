package succinct.util;

import java.util.Arrays;

public class BitVector64 implements ConstBlockBitVector {
  public static final int BLOCK_SIZE = 64;
  public static final long MASK = -1L;

  protected long[] blocks;
  protected int blockNum;

  @Override
  public void construct(long bitLength) {
    blockNum = (int)((bitLength-1L) / (long)BLOCK_SIZE) + 1;
    blocks = new long[blockNum];
    Arrays.fill(blocks, 0L);
  }
  @Override
  public void constructAsIndex(int blocksize) {
    blockNum = blocksize;
    blocks = new long[blockNum];
    Arrays.fill(blocks, 0L);
  }

  @Override
  public long access(int numBlock) {
    return blocks[numBlock];
  }

  private int popc(long r) {
    //System.out.printf("%x\n", r);
    r = (r & 0x5555555555555555L) + ((r & 0xaaaaaaaaaaaaaaaaL) >>> 1);
    r = (r & 0x3333333333333333L) + ((r & 0xccccccccccccccccL) >>> 2);
    r = (r & 0x0f0f0f0f0f0f0f0fL) + ((r & 0xf0f0f0f0f0f0f0f0L) >>> 4);
    r = (r & 0x00ff00ff00ff00ffL) + ((r & 0xff00ff00ff00ff00L) >>> 8);
    r = (r & 0x0000ffff0000ffffL) + ((r & 0xffff0000ffff0000L) >>> 16);
    r = (r & 0x00000000ffffffffL) + ((r & 0xffffffff00000000L) >>> 32);
    return (int) r;
  }

  @Override
  public int popcount(int numBlock) {
    return popc(blocks[numBlock]);
  }

  @Override
  public int popcount(int numBlock, int pos) {
    long mask = MASK >>> (BLOCK_SIZE - pos - 1);

    return popc(blocks[numBlock] & mask);
  }

  @Override
  public int select(int blockPos, int num) {
    int lest = num;
    long mask = 1L;
    long block = blocks[blockPos];
    for (int i=0; i<BLOCK_SIZE; i++) {
      if ((block & mask) == 1L) {
        num--;
        if (num == 0) {
          return i;
        }
      }
      block >>>= 1;
    }
    return -1;
  }

  @Override
  public void set(int pos, int i) {
    int blockPos = pos / BLOCK_SIZE;
    long bit = 1L << (pos % BLOCK_SIZE);

    if (i == 0) {
      blocks[blockPos] &= ~bit;
    } else {
      blocks[blockPos] |= bit;
    }
  }

  @Override
  public void setBlock(int block, long bits) {
    blocks[block] = bits;
  }

  @Override
  public int getBlockNum() { return blockNum; }
  @Override
  public int getBlockSize() { return BLOCK_SIZE; }
}
