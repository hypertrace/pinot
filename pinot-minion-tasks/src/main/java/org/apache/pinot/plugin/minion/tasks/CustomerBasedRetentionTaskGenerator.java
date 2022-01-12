package org.apache.pinot.plugin.minion.tasks;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.helix.ZNRecord;
import org.apache.helix.task.TaskState;
import org.apache.pinot.common.metadata.segment.OfflineSegmentZKMetadata;
import org.apache.pinot.controller.helix.core.minion.ClusterInfoAccessor;
import org.apache.pinot.controller.helix.core.minion.generator.PinotTaskGenerator;
import org.apache.pinot.controller.helix.core.minion.generator.TaskGeneratorUtils;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.spi.annotations.minion.TaskGenerator;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.TableTaskConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.apache.pinot.spi.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TaskGenerator
public class CustomerBasedRetentionTaskGenerator implements PinotTaskGenerator{

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerBasedRetentionTaskGenerator.class);
  private static final String TASK_TYPE = "customerBasedRetentionTask";
  public static final String BUCKET_TIME_PERIOD_KEY = "bucketTimePeriod";
  private static final String DEFAULT_BUCKET_PERIOD = "1d";
  private static final String CUSTOMER_RETENTION_CONFIG = "customerRetentionConfig";
  public static final String WINDOW_START_MS_KEY = "windowStartMs";
  public static final String WINDOW_END_MS_KEY = "windowEndMs";
  public static final int TABLE_MAX_NUM_TASKS = 1000; // vary accordingly

  private ClusterInfoAccessor _clusterInfoAccessor;

  @Override
  public void init(ClusterInfoAccessor clusterInfoAccessor) {
    _clusterInfoAccessor = clusterInfoAccessor;
  }

  @Override
  public String getTaskType() {
    return TASK_TYPE;
  }

  @Override
  public List<PinotTaskConfig> generateTasks(List<TableConfig> tableConfigs) {
    List<PinotTaskConfig> pinotTaskConfigs = new ArrayList<>();

    for (TableConfig tableConfig : tableConfigs) {
      String offlineTableName = tableConfig.getTableName();

      if (tableConfig.getTableType() != TableType.OFFLINE) {
        LOGGER.warn("Skip generating task: {} for non-OFFLINE table: {}", TASK_TYPE, offlineTableName);
        continue;
      }

      LOGGER.info("Start generating task configs for table: {} for task: {}", offlineTableName, TASK_TYPE);

      // Only schedule 1 job of this type (this optimisation may be moved downstream later on)
      Map<String, TaskState> incompleteTasks =
          TaskGeneratorUtils.getIncompleteTasks(TASK_TYPE, offlineTableName, _clusterInfoAccessor);
      if (!incompleteTasks.isEmpty()) {
        LOGGER
            .warn("Found incomplete tasks: {} for same table: {}. Skipping task generation.", incompleteTasks.keySet(),
                offlineTableName);
        continue;
      }

      TableTaskConfig tableTaskConfig = tableConfig.getTaskConfig();
      Preconditions.checkState(tableTaskConfig != null);
      Map<String, String> taskConfigs = tableTaskConfig.getConfigsForTaskType(TASK_TYPE);
      Preconditions.checkState(taskConfigs != null, "Task config shouldn't be null for table: {}", offlineTableName);

      // Get the bucket size
      String bucketTimePeriod =
          taskConfigs.getOrDefault(BUCKET_TIME_PERIOD_KEY, DEFAULT_BUCKET_PERIOD);
      long bucketMs = TimeUtils.convertPeriodToMillis(bucketTimePeriod);

      // Get watermark from OfflineSegmentsMetadata ZNode. WindowStart = watermark. WindowEnd = windowStart + bucket.
      long windowStartMs = getWatermarkMs(offlineTableName, bucketMs);
      long windowEndMs = windowStartMs + bucketMs;

      // Get customer retention config
      Map<String ,String> customerRetentionConfigMap = getCustomerRetentionConfig();
      String customerRetentionConfigMapString = customerRetentionConfigMap.keySet().stream()
          .map(key -> key + "=" + customerRetentionConfigMap.get(key))
          .collect(Collectors.joining(", ", "{", "}"));

      // Generate tasks
      for (OfflineSegmentZKMetadata offlineSegmentZKMetadata : _clusterInfoAccessor.getOfflineSegmentsMetadata(offlineTableName)) {
        // Only submit segments that have not been converted
        Map<String, String> customMap = offlineSegmentZKMetadata.getCustomMap();
        if (customMap == null || !customMap.containsKey(
            MinionConstants.ConvertToRawIndexTask.COLUMNS_TO_CONVERT_KEY + MinionConstants.TASK_TIME_SUFFIX)) {
          Map<String, String> configs = new HashMap<>();
          configs.put(MinionConstants.TABLE_NAME_KEY, offlineTableName);
          configs.put(MinionConstants.SEGMENT_NAME_KEY, offlineSegmentZKMetadata.getSegmentName());
          configs.put(MinionConstants.DOWNLOAD_URL_KEY, offlineSegmentZKMetadata.getDownloadUrl());
          configs.put(MinionConstants.UPLOAD_URL_KEY, _clusterInfoAccessor.getVipUrl() + "/segments");
          configs.put(MinionConstants.ORIGINAL_SEGMENT_CRC_KEY, String.valueOf(offlineSegmentZKMetadata.getCrc()));
          configs.put(CUSTOMER_RETENTION_CONFIG, customerRetentionConfigMapString);
          configs.put(WINDOW_START_MS_KEY, String.valueOf(windowStartMs));
          configs.put(WINDOW_END_MS_KEY, String.valueOf(windowEndMs));
          pinotTaskConfigs.add(new PinotTaskConfig(TASK_TYPE, configs));
        }
      }
      LOGGER.info("Finished generating task configs for table: {} for task: {}", offlineTableName, TASK_TYPE);
    }
    return pinotTaskConfigs;
  }

  private long getWatermarkMs(String offlineTableName, long bucketMs){
    List<OfflineSegmentZKMetadata> offlineSegmentZKMetadataList =
        _clusterInfoAccessor.getOfflineSegmentsMetadata(offlineTableName);
    CustomerBasedRetentionTaskMetadata customerBasedRetentionTaskMetadata =
        getCustomerBasedRetentionTaskMetadata(offlineTableName);

    // No ZNode exists. Cold-start.
    if(customerBasedRetentionTaskMetadata == null){

      // Find the smallest time from all segments
      long minStartTimeMs = Long.MAX_VALUE;
      for (OfflineSegmentZKMetadata offlineSegmentZKMetadata : offlineSegmentZKMetadataList) {
        minStartTimeMs = Math.min(minStartTimeMs, offlineSegmentZKMetadata.getStartTimeMs());
      }
      Preconditions.checkState(minStartTimeMs != Long.MAX_VALUE);

      // Round off according to the bucket. This ensures we align the offline segments to proper time boundaries
      // For example, if start time millis is 20200813T12:34:59, we want to create the first segment for window [20200813, 20200814)
      long watermarkMs = (minStartTimeMs / bucketMs) * bucketMs;

      // Create CustomerBasedRetentionTaskMetadata ZNode using watermark calculated above
      customerBasedRetentionTaskMetadata = new CustomerBasedRetentionTaskMetadata(offlineTableName, watermarkMs);
      setCustomerBasedRetentionTaskMetadata(customerBasedRetentionTaskMetadata);
    }

    return customerBasedRetentionTaskMetadata.getWatermarkMs();
  }

  private Map<String,String> getCustomerRetentionConfig(){
    // add code here
    Map<String,String> customerRetentionConfig = new HashMap<>();
    return customerRetentionConfig;
  }

  private CustomerBasedRetentionTaskMetadata getCustomerBasedRetentionTaskMetadata(String offlineTableName){

  }

  private void setCustomerBasedRetentionTaskMetadata(CustomerBasedRetentionTaskMetadata customerBasedRetentionTaskMetadata){

  }

  public class CustomerBasedRetentionTaskMetadata {
    private static final String WATERMARK_KEY = "watermarkMs";

    private final String _tableNameWithType;
    private final long _watermarkMs;

    public CustomerBasedRetentionTaskMetadata(String tableNameWithType, long watermarkMs) {
      _tableNameWithType = tableNameWithType;
      _watermarkMs = watermarkMs;
    }

    public CustomerBasedRetentionTaskMetadata fromZNRecord(ZNRecord znRecord) {
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
}