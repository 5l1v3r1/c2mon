package cern.c2mon.server.common.cache;

import cern.c2mon.server.common.commfault.CommFaultTagCacheObject;

public class CommFaultTagCacheObjectTest extends CacheObjectTest<CommFaultTagCacheObject> {

  private static CommFaultTagCacheObject sample = new CommFaultTagCacheObject(1L);

  public CommFaultTagCacheObjectTest() {
    super(sample);
  }
}