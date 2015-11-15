package succinct;

import java.util.Map;
import succinct.util.ConstBlockBitVector;

public interface SuccinctBitVector {
  public long select(long i);
  public long rank(long i);

  public void build(ConstBlockBitVector bv, Map config);
}
