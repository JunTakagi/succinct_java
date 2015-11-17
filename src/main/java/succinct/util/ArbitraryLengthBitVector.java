package succinct.util;

import java.util.Arrays;

public class ArbitraryLengthBitVector {
  public static final long MASK = -1L;

  protected long[] blocks;
  int cursor;
  int curBlock;

  public ArbitraryLengthBitVector() { }

  public ArbitraryLengthBitVector(long bitLength) {
    this.construct(bitLength);
  }

  public void copy(BitVector64 origin) {
    this.blocks = Arrays.copyOf(origin.blocks, origin.blocks.length);
  }

  public void construct(long bitLength) {
    int blockNum = (int)((bitLength-1L) / 64L) + 1;
    blocks = new long[blockNum];
    Arrays.fill(blocks, 0L);
    cursor=0;
    curBlock=0;
  }

  public long access(long start, long end) {
    int sblock = (int)(start / 64L);
    int endblock = (int)(end / 64L);
    int shift = (int)(start % 64L);
    int size = (int)(end - start) + 1;

    if (sblock == endblock) {
      long block = blocks[sblock];

      long mask = size >=64 ? MASK:~(MASK << size);
      return (block >>> shift) & mask;
    }

    int lowbitsize = 64 - shift;

    long lowbits = (blocks[sblock] >>> shift) & ~(MASK << lowbitsize);
    long highbitmask = size >=64 ? MASK:~(MASK << size);
    long highbits = (blocks[endblock] << lowbitsize) & highbitmask;

    return highbits | lowbits;
  }

  public void setNext(int bit) {
    blocks[curBlock] |= ((long)(bit) & 1L) << cursor;
    cursor++;
    if (cursor == 64) {
      cursor = 0;
      curBlock++;
    }
  }

  public long size() { return 64L * (long)blocks.length; }

  public void dump() {
    for (int i=0; i<blocks.length; i++) {
      System.out.println(Long.toHexString(blocks[i]));
    }
  }
}
