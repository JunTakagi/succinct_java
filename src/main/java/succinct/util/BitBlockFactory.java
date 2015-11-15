package succinct.util;

/**
 * block, block内のbit位置など、全てのindexは0から始める
 */
public class BitBlockFactory {
  public static ConstBlockBitVector getUpTo(int maxBitSize) {
    return new BitVector64();
  }
}
