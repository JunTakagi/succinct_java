package succinct.rrr;

import java.util.Map;
import java.util.HashMap;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import succinct.util.*;

public class RRRTest {
  private RRR bv;
  private Map conf;

  @BeforeMethod
  public void prepare() {
    conf = new HashMap();
    conf.put(RRR.CONF_SUPERBLOCK_STEP, new Integer(4));
    conf.put(RRR.CONF_BLOCK_SIZE, new Integer(63));
    conf.put(RRR.CONF_R_SAMPLE_STEP, new Integer(4));

    BitVector64 origin = new BitVector64();
    origin.construct(64L * 32L);

    long testBlock1 = 0xf0000000ffffffffL; // rank 36
    long testBlock2 = 0x0101010101010101L; // rank 8
    long testBlock3 = 0xf0f0f0f0f0f0f0f0L; // rank 32
    long testBlock4 = 0xf0f0f0f0f0f0f0f0L; // rank 32

    for (int i=0; i<32; i+=4) {
      origin.setBlock(i, testBlock1);
      origin.setBlock(i+1, testBlock2);
      origin.setBlock(i+2, testBlock3);
      origin.setBlock(i+3, testBlock4);
    }

    bv = new RRR();
    bv.build(origin, conf);
  }

  @Test
  public void testRank() {
    // first block
    assertEquals(bv.rank(0L), 1L);
    assertEquals(bv.rank(1L), 2L);
    assertEquals(bv.rank(63L), 36L);
    // second block
    assertEquals(bv.rank(64L), 37L);
    assertEquals(bv.rank(65L), 37L);
    assertEquals(bv.rank(71L), 37L);
    assertEquals(bv.rank(72L), 38L);
    // third block
    assertEquals(bv.rank(128L), 44L);

    // second super block
    assertEquals(bv.rank(256L), 109L);
    // third super block
    assertEquals(bv.rank(512L), 217L);
    // fourth super block
    assertEquals(bv.rank(768L), 325L);
    // fourth super block, first block
    assertEquals(bv.rank(799L), 356L);
    // fourth super block, second block
    assertEquals(bv.rank(840L), 362L);
  }
  @Test
  public void testSelect() {
    // first block
    assertEquals(bv.select(1L), 0L);
    assertEquals(bv.select(2L), 1L);
    assertEquals(bv.select(32L), 31L);
    assertEquals(bv.select(33L), 60L);
    // second block
    assertEquals(bv.select(37L), 64L);
    assertEquals(bv.select(44L), 120L);
    // third block
    assertEquals(bv.select(45L), 132L);
    assertEquals(bv.select(76L), 191L);
    // fourth block
    assertEquals(bv.select(77L), 196L);
    assertEquals(bv.select(108L), 255L);

    // second super block
    assertEquals(bv.select(109L), 256L);
    // second super block, second block
    assertEquals(bv.select(150L), 360L);
    // second super block, third block
    assertEquals(bv.select(153L), 388L);

    // third super block
    assertEquals(bv.select(217L), 512L);
    // third super block, fourth block
    assertEquals(bv.select(293L), 708L);

    // fourth super block
    assertEquals(bv.select(325L), 768L);
    // fourth super block, third block
    assertEquals(bv.select(400L), 959L);
    // last
    assertEquals(bv.select(432L), 1023L);
  }

}
