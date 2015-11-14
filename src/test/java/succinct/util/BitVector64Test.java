package succinct.util;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class BitVector64Test {
  private BitVector64 bv;

  @BeforeMethod
  public void prepare() {
    long testBlock1 = 0xf0000000ffffffffL;
    long testBlock2 = 0x0101010101010101L;
    long testBlock3 = 0xf0f0f0f0f0f0f0f0L;

    bv = new BitVector64();
    bv.construct(64L * 3L);
    bv.setBlock(0, testBlock1);
    bv.setBlock(1, testBlock2);
    bv.setBlock(2, testBlock3);
  }

  @AfterMethod
  public void clean() {
    bv = null;
  }

  @Test
  public void testConstructAndNum() {
    assertEquals(bv.getBlockNum(), 3);
  }

  @Test
  public void testSelect() {
    assertEquals(bv.select(0, 1), 0);
    assertEquals(bv.select(0, 32), 31);
    assertEquals(bv.select(0, 33), 60);
    assertEquals(bv.select(1, 3), 16);
    assertEquals(bv.select(2, 8), 15);
  }

  @Test
  public void testPopCount() {
    assertEquals(bv.popcount(0), 36);
    assertEquals(bv.popcount(0, 27), 28);
    assertEquals(bv.popcount(1), 8);
    assertEquals(bv.popcount(1, 7), 1);
    assertEquals(bv.popcount(1, 8), 2);
    assertEquals(bv.popcount(2), 32);
  }
}
