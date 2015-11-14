package succinct.util;

/**
 * block, block内のbit位置など、全てのindexは0から始める
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
