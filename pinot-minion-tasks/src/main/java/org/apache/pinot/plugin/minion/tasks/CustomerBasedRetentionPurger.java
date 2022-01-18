package org.apache.pinot.plugin.minion.tasks;

import org.apache.pinot.core.minion.SegmentPurger.RecordPurger;
import org.apache.pinot.spi.data.readers.GenericRow;
import java.util.Map;

public class CustomerBasedRetentionPurger implements RecordPurger {

  private final Map<String, String> filterConfig;

  public CustomerBasedRetentionPurger(Map<String, String> filterConfig) {
    this.filterConfig = filterConfig;
  }

  @Override
  public boolean shouldPurge(GenericRow row) {
    for(Map.Entry<String, String> filter : filterConfig.entrySet()) {

    }
    return false;
  }
}