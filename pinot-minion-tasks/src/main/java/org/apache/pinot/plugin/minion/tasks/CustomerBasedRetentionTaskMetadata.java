package org.apache.pinot.plugin.minion.tasks;

import org.apache.helix.ZNRecord;

// Add functions on ad hoc basis in this class
public class CustomerBasedRetentionTaskMetadata {
  private static final String WATERMARK_KEY = "watermarkMs";

  private final String _tableNameWithType;
  private final long _watermarkMs;

  public CustomerBasedRetentionTaskMetadata(String tableNameWithType, long watermarkMs) {
    _tableNameWithType = tableNameWithType;
    _watermarkMs = watermarkMs;
  }

  public static CustomerBasedRetentionTaskMetadata fromZNRecord(
      ZNRecord znRecord) {
    long watermark = znRecord.getLongField(WATERMARK_KEY, 0);
    return new CustomerBasedRetentionTaskMetadata(znRecord.getId(), watermark);
  }

  /**
   * Get the watermark in millis
   */
  public long getWatermarkMs() {
    return _watermarkMs;
  }
}
