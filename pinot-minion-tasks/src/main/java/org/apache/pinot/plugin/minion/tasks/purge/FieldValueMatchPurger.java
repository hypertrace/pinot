package org.apache.pinot.plugin.minion.tasks.purge;

import org.apache.pinot.core.minion.SegmentPurger.RecordPurger;
import org.apache.pinot.spi.data.readers.GenericRow;

public class FieldValueMatchPurger implements RecordPurger {
  @Override
  public boolean shouldPurge(GenericRow row) {
    return false;
  }
}
