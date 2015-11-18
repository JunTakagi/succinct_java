package succinct.rrr;

import java.util.Map;
import java.util.HashMap;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import succinct.util.*;

public class RCalculationTest {
  private RRR rrr;
  private Map conf;
  private BitVector64 dummy;

  @BeforeMethod
  public void prepare() {
    conf = new HashMap();
    dummy = new BitVector64();
    dummy.construct(64L * 32L);
    rrr = new RRR();
  }
  @AfterMethod
  public void clean() {
    conf.clear();
  }

  /**
   * このテストはさぼり過ぎていて酷い
   */
  @Test
  public void testRCalculation() {
    int blocksize = 4;
    conf.put(RRR.CONF_BLOCK_SIZE, new Integer(blocksize));
    rrr.build(dummy, conf);

    long maxval = 1L << blocksize -1L;
    for (long i=0; i<maxval; i++) {
      int klass = rrr.popcount(i);
      long r = rrr.calcR(klass, i);
      long block = rrr.decodeR(klass, r);

      assertEquals(i, block);

      int rLength = rrr.getRLength(klass);
      long maxRValue = (1L << rLength) -1L;

      assertTrue(r <= maxRValue);
    }
  }

  /**
   * rの計算途中が最大になる値で桁溢れが生じないことを確認
   */
  @Test
  public void testMaxRCalculate() {
    int blocksize = 62;
    conf.put(RRR.CONF_BLOCK_SIZE, new Integer(blocksize));
    rrr.build(dummy, conf);

    int klass = 31;
    long block = 0x3fffffff80000000L;
    long r = rrr.calcR(klass, block);
    long decoded = rrr.decodeR(klass, r);
    assertEquals(decoded, block);
  }
}
