package org.apache.pinot.minion.tasks.purger;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.helix.task.TaskState;
import org.apache.pinot.common.metadata.segment.OfflineSegmentZKMetadata;
import org.apache.pinot.common.metadata.segment.SegmentZKMetadata;
import org.apache.pinot.controller.helix.core.minion.ClusterInfoAccessor;
import org.apache.pinot.controller.helix.core.minion.generator.PinotTaskGenerator;
import org.apache.pinot.controller.helix.core.minion.generator.TaskGeneratorUtils;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.minion.tasks.MinionTaskConstants;
import org.apache.pinot.spi.annotations.minion.TaskGenerator;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.TableTaskConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@TaskGenerator
public class CustomPurgeTaskGenerator implements PinotTaskGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomPurgeTaskGenerator.class);
  private static final String TASK_TYPE = MinionTaskConstants.CusmtomPurgeTask.TASK_TYPE;

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

      LOGGER.info(
          "Start generating task configs for table: {} for task: {}", offlineTableName, TASK_TYPE);

      // Only generate tasks for OFFLINE tables
      if (tableConfig.getTableType() != TableType.OFFLINE) {
        LOGGER.warn(
            "Skip generating task: {} for non-OFFLINE table: {}", TASK_TYPE, offlineTableName);
        continue;
      }

      // Only schedule 1 job of this type (this optimisation may be moved downstream later on)
      Map<String, TaskState> incompleteTasks =
          TaskGeneratorUtils.getIncompleteTasks(TASK_TYPE, offlineTableName, _clusterInfoAccessor);
      if (!incompleteTasks.isEmpty()) {
        LOGGER.warn(
            "Found incomplete tasks: {} for same table: {}. Skipping task generation.",
            incompleteTasks.keySet(),
            offlineTableName);
        continue;
      }

      TableTaskConfig tableTaskConfig = tableConfig.getTaskConfig();
      Preconditions.checkNotNull(tableTaskConfig);
      Map<String, String> taskConfigs = tableTaskConfig.getConfigsForTaskType(TASK_TYPE);
      Preconditions.checkNotNull(
          taskConfigs, "Task config shouldn't be null for Table: {}", offlineTableName);

      CustomPurgeTaskConfig purgeTaskConfig = CustomPurgeTaskConfig.fromMap(taskConfigs);
      TreeSet<String> filteredSegments = new TreeSet<>(purgeTaskConfig.getSegmentNames());

      TreeSet<OfflineSegmentZKMetadata> segmentMetadataSet =
          new TreeSet<>(Comparator.comparing(SegmentZKMetadata::getSegmentName));
      segmentMetadataSet.addAll(_clusterInfoAccessor.getOfflineSegmentsMetadata(offlineTableName));

      // Parallel collections - acts as a state for each task.
      List<String> segmentNames = new ArrayList<>();
      List<String> downloadURLs = new ArrayList<>();
      List<String> originalSegmentCRCs = new ArrayList<>();

      for (OfflineSegmentZKMetadata segmentMetadata : segmentMetadataSet) {
        if (filteredSegments.contains(segmentMetadata.getSegmentName())) {
          segmentNames.add(segmentMetadata.getSegmentName());
          downloadURLs.add(segmentMetadata.getDownloadUrl());
          originalSegmentCRCs.add(String.valueOf(segmentMetadata.getCrc()));
        }

        // Each task should process maxSegmentsPerTask config
        if (segmentNames.size() >= purgeTaskConfig.getMaxSegmentsPerTask()) {
          Map<String, String> configs = new HashMap<>();

          configs.put(MinionConstants.TABLE_NAME_KEY, offlineTableName);
          configs.put(MinionConstants.SEGMENT_NAME_KEY, StringUtils.join(segmentNames, ","));
          configs.put(
              MinionConstants.DOWNLOAD_URL_KEY,
              StringUtils.join(downloadURLs, MinionConstants.URL_SEPARATOR));
          configs.put(
              MinionConstants.UPLOAD_URL_KEY, _clusterInfoAccessor.getVipUrl() + "/segments");
          configs.put(
              MinionConstants.ORIGINAL_SEGMENT_CRC_KEY, StringUtils.join(originalSegmentCRCs, ","));

          // send original task configs
          configs.putAll(taskConfigs);
          pinotTaskConfigs.add(new PinotTaskConfig(TASK_TYPE, configs));

          // Clear the state for next task
          segmentNames = new ArrayList<>();
          downloadURLs = new ArrayList<>();
          originalSegmentCRCs = new ArrayList<>();
        }
      }

      LOGGER.info(
          "Finished generating task configs for table: {} for task: {}",
          offlineTableName,
          TASK_TYPE);
    }
    return pinotTaskConfigs;
  }
}
