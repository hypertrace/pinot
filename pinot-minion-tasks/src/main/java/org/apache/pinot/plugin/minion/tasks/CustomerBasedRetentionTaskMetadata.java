package org.apache.pinot.plugin.minion.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.helix.ZNRecord;
import org.apache.pinot.spi.utils.JsonUtils;

/**
 * Metadata for the minion task of type CustomerBasedRetentionTask.
 * The <code>watermarkMs</code> denotes the time (exclusive) upto which tasks have been executed.
 *
 * This gets serialized and stored in zookeeper under the path MINION_TASK_METADATA/CustomerBasedRetentionTask/tableNameWithType
 *
 * PinotTaskGenerator:
 * The <code>watermarkMs</code>> is used by the <code>CustomerBasedRetentionTaskGenerator</code>,
 * to determine the window of execution for the task it is generating.
 * The window of execution will be [watermarkMs, watermarkMs + bucketSize)
 *
 * PinotTaskExecutor:
 * The same watermark is used by the <code>CustomerBasedRetentionTaskExecutor</code>, to:
 * - Verify that is running the latest task scheduled by the task generator
 * - Update the watermark as the end of the window that it executed for
 */
public class CustomerBasedRetentionTaskMetadata {
  private static final String WATERMARK_KEY = "watermarkMs";

  private final String _tableNameWithType;
  private final long _watermarkMs;

  public CustomerBasedRetentionTaskMetadata(String tableNameWithType, long watermarkMs) {
    _tableNameWithType = tableNameWithType;
    _watermarkMs = watermarkMs;
  }

  public String getTableNameWithType() {
    return _tableNameWithType;
  }

  /**
   * Get the watermark in millis
   */
  public long getWatermarkMs() {
    return _watermarkMs;
  }

  public static CustomerBasedRetentionTaskMetadata fromZNRecord(
      ZNRecord znRecord) {
    long watermark = znRecord.getLongField(WATERMARK_KEY, 0);
    return new CustomerBasedRetentionTaskMetadata(znRecord.getId(), watermark);
  }

  public ZNRecord toZNRecord() {
    ZNRecord znRecord = new ZNRecord(_tableNameWithType);
    znRecord.setLongField(WATERMARK_KEY, _watermarkMs);
    return znRecord;
  }

  public String toJsonString() {
    try {
      return JsonUtils.objectToString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String toString() {
    return toJsonString();
  }
}
