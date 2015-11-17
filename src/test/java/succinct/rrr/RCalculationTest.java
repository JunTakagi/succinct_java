package succinct.rrr;

import java.util.Map;
import java.util.HashMap;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import succinct.util.*;

public class RCalculationTest {
  private RRR bv;
  private Map conf;

  @BeforeMethod
  public void prepare() {
  }

  /**
   * このテストはさぼり過ぎていて酷い
   */
  @Test
  public void testRCalculation() {
    int blocksize = 4;
    long maxval = 1L << blocksize -1L;
    for (long i=0; i<maxval; i++) {
      int klass = RRR.popcount(i);
      long r = RRR.calcR(blocksize, klass, i);
      long block = RRR.decodeR(blocksize, klass, r);

      assertEquals(i, block);

      int rLength = RRR.calcRLength(blocksize, klass);
      long maxRValue = (1L << rLength) -1L;

      assertTrue(r <= maxRValue);
    }

    blocksize = 63;
    long block = 0x7331cc5599aae8b2L;
    long r = RRR.calcR(blocksize, 32, block);
    long decoded = RRR.decodeR(blocksize, 32, r);
    assertEquals(decoded, block);
  }
}
