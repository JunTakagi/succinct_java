package succinct.util;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class BitVectorNTest {
  private BitVectorN bv;
  private int[] data = {127, 0, 56, 89, 100, 120, 25, 38};

  @BeforeMethod
  public void prepare() {
    try {
    bv = new BitVectorN(7); // 0~127
    bv.construct(80);
    for (int i=0; i<10; i++) {
      for (int j=0; j<data.length; j++) bv.setNext(data[j]);
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
    for (int j=0; j<data.length; j++) assertEquals(bv.access(j), (long)data[j]);
    // on boundary
    assertEquals(bv.access(9), (long)data[9 % data.length]);
    assertEquals(bv.access(18), (long)data[18 % data.length]);
    assertEquals(bv.access(27), (long)data[27 % data.length]);
    assertEquals(bv.access(37), (long)data[37 % data.length]);
    assertEquals(bv.access(45), (long)data[45 % data.length]);
    assertEquals(bv.access(54), (long)data[54 % data.length]);
    assertEquals(bv.access(64), (long)data[64 % data.length]);
    assertEquals(bv.access(73), (long)data[73 % data.length]);
  }
}
