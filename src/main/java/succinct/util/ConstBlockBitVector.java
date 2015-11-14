package succinct.util;

/**
 * block, block$BFb$N(Bbit$B0LCV$J$I!"A4$F$N(Bindex$B$O(B0$B$+$i;O$a$k(B
 */
public interface ConstBlockBitVector {
  public void construct(long bitLength);
  public long access(int numBlock);
  public int select(int blockPos, int num);
  public int popcount(int numBlock);
  public int popcount(int numBlock, int pos);
  public void set(int pos, int i);
  public void setBlock(int block, long bits);
  public int getBlockNum();
}
