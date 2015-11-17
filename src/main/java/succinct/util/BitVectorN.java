package succinct.util;

import java.util.Arrays;

public class BitVectorN {
  public static final long LONGMASK = -1L;

  protected long[] vector;
  int cursor;
  int curBlock;
  int wordsize;
  long mask;

  public BitVectorN(int wordsize) {
    this.wordsize = wordsize;
    mask=(1L<<wordsize)-1L;
  }

  public void copy(BitVector64 origin) {
    this.vector = Arrays.copyOf(origin.blocks, origin.blocks.length+1); // extra 1 block is needed for padding of last block
  }

  public void construct(int blocknum) {
    long bitLength = (long)(blocknum) * (long)(wordsize);
    int blockNum = (int)((bitLength-1L) / 64L) + 1;
    vector = new long[blockNum];
    Arrays.fill(vector, 0L);
    cursor=0;
  }

  public long access(int block) {
    int start = block * wordsize;
    int end = start + wordsize - 1;
    int sblock = (int)(start / 64L);
    int endblock = (int)(end / 64L);
    int shift = (int)(start % 64L);
    int size = (int)(end - start) + 1;

    if (sblock == endblock) {
      long bits = vector[sblock];

      return (bits >>> shift) & mask;
    }

    int lowbitsize = 64 - shift;

    long lowbits = (vector[sblock] >>> shift) & ~(LONGMASK << lowbitsize);
    long highbits = (vector[endblock] << lowbitsize) & mask;

    return highbits | lowbits;
  }

  public void setNext(int bit) {
    long add = mask & (long)bit;

    int start = cursor * wordsize;
    int end = start + wordsize - 1;
    int sblock = (int)(start / 64L);
    int endblock = (int)(end / 64L);
    int shift = (int)(start % 64L);
    int size = (int)(end - start) + 1;

    if (sblock == endblock) {
      vector[sblock] |= add << shift;
    } else {
      vector[sblock] |= add << shift;

      long highbits = add >>> (64 - shift);

      vector[endblock] |= highbits;
    }
    cursor++;
  }

  public long size() { return 64L * (long)vector.length; }

  public void dump() {
    for (int i=0; i<vector.length; i++) {
      System.out.println(Long.toHexString(vector[i]));
    }
  }
}
