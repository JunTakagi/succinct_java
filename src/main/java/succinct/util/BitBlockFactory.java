package succinct.util;

/**
 * block, block$BFb$N(Bbit$B0LCV$J$I!"A4$F$N(Bindex$B$O(B0$B$+$i;O$a$k(B
 */
public class BitBlockFactory {
  public static ConstBlockBitVector getUpTo(int maxBitSize) {
    return new BitVector64();
  }
}
