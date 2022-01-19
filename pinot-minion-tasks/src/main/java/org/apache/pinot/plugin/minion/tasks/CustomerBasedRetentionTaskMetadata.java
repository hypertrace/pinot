package org.apache.pinot.plugin.minion.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
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
  private final Map<String, String> _watermarkMsMap;

  public CustomerBasedRetentionTaskMetadata(String tableNameWithType, Map<String ,String> watermarkMs) {
    _tableNameWithType = tableNameWithType;
    _watermarkMsMap = watermarkMs;
  }

  public String getTableNameWithType() {
    return _tableNameWithType;
  }

  /**
   * Get the watermark in millis
   */
  public Map<String, String> getWatermarkMsMap() {
    return _watermarkMsMap;
  }

  public static CustomerBasedRetentionTaskMetadata fromZNRecord (ZNRecord znRecord) {
    Map<String,String> watermark = znRecord.getMapField(WATERMARK_KEY);
    return new CustomerBasedRetentionTaskMetadata(znRecord.getId(), watermark);
  }

  public ZNRecord toZNRecord() {
    ZNRecord znRecord = new ZNRecord(_tableNameWithType);
    znRecord.setMapField(WATERMARK_KEY, _watermarkMsMap);
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
