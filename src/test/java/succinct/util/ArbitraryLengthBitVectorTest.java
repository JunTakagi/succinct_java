package succinct.util;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class ArbitraryLengthBitVectorTest {
  private ArbitraryLengthBitVector abv;

  @BeforeMethod
  public void prepare() {
    try {
    int[] bits1 = {1, 0, 0, 1, 0, 1, 1, 1}; // 9, 7
    int[] bits2 = {0, 1, 1, 0, 0, 1, 1, 1}; // 6, 7
    int[] bits3 = {1, 0, 1, 0, 1, 1, 0, 1}; // a, d
    int[] bits4 = {0, 0, 0, 1, 0, 0, 0, 1}; // 1, 1

    abv = new ArbitraryLengthBitVector();
    abv.construct(8L * 4L * 2L * 10L);
    for (int i=0; i<20; i++) {
      for (int j=bits1.length-1; j>=0; j--) abv.setNext(bits1[j]);
      for (int j=bits2.length-1; j>=0; j--) abv.setNext(bits2[j]);
      for (int j=bits3.length-1; j>=0; j--) abv.setNext(bits3[j]);
      for (int j=bits4.length-1; j>=0; j--) abv.setNext(bits4[j]);
    }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
      System.out.println(e.getClass().getName());
    }
  }

  @Test
  public void testAccess() {
    // first block
    assertEquals(abv.access(0,7), Long.parseLong("10010111", 2));
    assertEquals(abv.access(8,15), Long.parseLong("01100111", 2));
    assertEquals(abv.access(16,23), Long.parseLong("10101101", 2));
    assertEquals(abv.access(24,31), Long.parseLong("00010001", 2));
    // second block
    assertEquals(abv.access(96,103), Long.parseLong("10010111", 2));
    assertEquals(abv.access(112,127), Long.parseLong("0001000110101101", 2));
    // third block
    assertEquals(abv.access(128 + 10,128 + 27), Long.parseLong("000110101101011001", 2));
    assertEquals(abv.access(128 + 10,128 + 28), Long.parseLong("1000110101101011001", 2));

    // first and second block
    assertEquals(abv.access(56, 71), Long.parseLong("1001011100010001", 2));
    assertEquals(abv.access(48, 111), 0x679711ad679711adL);
    // second and third block
    assertEquals(abv.access(96 + 23, 128 + 11), Long.parseLong("11110010111000100011", 2));

    // last block
    assertEquals(abv.access(576, 639), 0x11ad679711ad6797L);
  }
}
