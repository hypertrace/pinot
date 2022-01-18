package org.apache.pinot.minion.tasks.purger;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.pinot.core.minion.SegmentPurger;
import org.apache.pinot.core.minion.SegmentPurger.RecordPurger;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FieldValueMatchPurger implements RecordPurger {
  private static final Logger LOGGER = LoggerFactory.getLogger(SegmentPurger.class);
  private final RateLimiter rateLimiter = RateLimiter.create(0.016);
  private final Map<String, String> filterConfig;

  public FieldValueMatchPurger(Map<String, String> filterConfig) {
    this.filterConfig = filterConfig;
  }

  @Override
  public boolean shouldPurge(GenericRow row) {
    for(Map.Entry<String, String> filter : filterConfig.entrySet()) {
      if(rateLimiter.tryAcquire()) {
        LOGGER.info("value type: {}, column value: {}, filter value: {}", row.getValue(filter.getKey()));
      }
      // TODO: fix it
      // Comparing string value using toString() is not correct.
      if(row.getValue(filter.getKey()) == null || row.getValue(filter.getKey()).toString().equals(filter.getValue()) == false) {
        return false;
      }
    }
    return true;
  }
}
