package org.apache.pinot.plugin.minion.tasks;

import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.TASK_TYPE;
import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.WINDOW_START_MS_KEY;

import com.google.common.base.Preconditions;
import java.lang.reflect.Field;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.helix.ZNRecord;
import org.apache.helix.store.HelixPropertyStore;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.pinot.common.metadata.segment.SegmentZKMetadataCustomMapModifier;
import org.apache.pinot.common.minion.MinionTaskMetadataUtils;
import org.apache.pinot.common.utils.FileUploadDownloadClient;
import org.apache.pinot.common.utils.TarGzCompressionUtils;
import org.apache.pinot.common.utils.fetcher.SegmentFetcherFactory;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.core.minion.SegmentPurger;
import org.apache.pinot.minion.exception.TaskCancelledException;
import org.apache.pinot.minion.executor.BaseTaskExecutor;
import org.apache.pinot.minion.executor.MinionTaskZkMetadataManager;
import org.apache.pinot.minion.executor.SegmentConversionResult;
import org.apache.pinot.minion.executor.SegmentConversionUtils;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.utils.builder.TableNameBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerBasedRetentionTaskExecutor extends BaseTaskExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerBasedRetentionTaskExecutor.class);
  private static final String SEGMENT_CRC_SEPARATOR = ",";
  private static final String TASK_TIME_SUFFIX = ".time";
  public static final String RECORD_PURGER_KEY = "recordPurger";
  public static final String RECORD_MODIFIER_KEY = "recordModifier";
  public static final String NUM_RECORDS_PURGED_KEY = "numRecordsPurged";
  public static final String NUM_RECORDS_MODIFIED_KEY = "numRecordsModified";
  private final MinionTaskZkMetadataManager _minionTaskZkMetadataManager;

  private HelixPropertyStore<ZNRecord> propertyStore;
  private int _expectedVersion = Integer.MIN_VALUE;
  private long _nextWatermark;

  public CustomerBasedRetentionTaskExecutor(MinionTaskZkMetadataManager minionTaskZkMetadataManager) {
    _minionTaskZkMetadataManager = minionTaskZkMetadataManager;
  }

  @Override
  public List<SegmentConversionResult> executeTask(PinotTaskConfig pinotTaskConfig)
      throws Exception {
    preProcess(pinotTaskConfig);

    String taskType = pinotTaskConfig.getTaskType();
    Map<String, String> configs = pinotTaskConfig.getConfigs();
    String tableNameWithType = configs.get(
        MinionConstants.TABLE_NAME_KEY);
    String inputSegmentNames = configs.get(MinionConstants.SEGMENT_NAME_KEY);
    String downloadURLString = configs.get(MinionConstants.DOWNLOAD_URL_KEY);
    String[] downloadURLs = downloadURLString.split(MinionConstants.URL_SEPARATOR);
    String originalSegmentCRCString = configs.get(MinionConstants.ORIGINAL_SEGMENT_CRC_KEY);
    String[] originalSegmentCRCs = originalSegmentCRCString.split(SEGMENT_CRC_SEPARATOR);
    String uploadURL = configs.get(MinionConstants.UPLOAD_URL_KEY);

    LOGGER.info("Start executing {} on table: {}, input segments: {} with downloadURLs: {}, uploadURL: {}", taskType,
        tableNameWithType, inputSegmentNames, downloadURLString, uploadURL);

    File tempDataDir = new File(new File(MINION_CONTEXT.getDataDir(), taskType), "tmp-" + UUID.randomUUID());
    Preconditions.checkState(tempDataDir.mkdirs());
    String crypterName = getTableConfig(tableNameWithType).getValidationConfig().getCrypterClassName();

    try {

      File workingDir = new File(tempDataDir, "workingDir");
      Preconditions.checkState(workingDir.mkdir());
      List<SegmentConversionResult> segmentConversionResults = new ArrayList<>();

      for (int i = 0; i < downloadURLs.length; i++) {
        // Download the segment file
        File tarredSegmentFile = new File(tempDataDir, "tarredSegmentFile_" + i);
        LOGGER.info("Downloading segment from {} to {}", downloadURLs[i], tarredSegmentFile.getAbsolutePath());
        SegmentFetcherFactory.fetchAndDecryptSegmentToLocal(downloadURLs[i], tarredSegmentFile, crypterName);

        // Un-tar the segment file
        File segmentDir = new File(tempDataDir, "segmentDir_" + i);
        File indexDir = TarGzCompressionUtils.untar(tarredSegmentFile, segmentDir).get(0);

        // Convert the segments
        segmentConversionResults.add(convert(pinotTaskConfig, indexDir, workingDir));
      }

      // Create a directory for converted tarred segment files
      File convertedTarredSegmentDir = new File(tempDataDir, "convertedTarredSegmentDir");
      Preconditions.checkState(convertedTarredSegmentDir.mkdir());

      int numOutputSegments = segmentConversionResults.size();
      List<File> tarredSegmentFiles = new ArrayList<>(numOutputSegments);
      for (SegmentConversionResult segmentConversionResult : segmentConversionResults) {
        // Tar the converted segment
        File convertedIndexDir = segmentConversionResult.getFile();
        File convertedSegmentTarFile = new File(convertedTarredSegmentDir,
            segmentConversionResult.getSegmentName() + TarGzCompressionUtils.TAR_GZ_FILE_EXTENSION);
        TarGzCompressionUtils.createTarGzFile(convertedIndexDir, convertedSegmentTarFile);
        tarredSegmentFiles.add(convertedSegmentTarFile);
      }

      // Check whether the task get cancelled before uploading the segment
      if (_cancelled) {
        LOGGER.info("{} on table: {}, segments: {} got cancelled", taskType, tableNameWithType, inputSegmentNames);
        throw new TaskCancelledException(
            taskType + " on table: " + tableNameWithType + ", segments: " + inputSegmentNames + " got cancelled");
      }

      // Upload the tarred segments
      for (int i = 0; i < numOutputSegments; i++) {
        File convertedTarredSegmentFile = tarredSegmentFiles.get(i);
        String resultSegmentName = segmentConversionResults.get(i).getSegmentName();

        // Set parameters for upload request
        NameValuePair enableParallelPushProtectionParameter =
            new BasicNameValuePair(
                FileUploadDownloadClient.QueryParameters.ENABLE_PARALLEL_PUSH_PROTECTION, "true");
        NameValuePair tableNameParameter = new BasicNameValuePair(FileUploadDownloadClient.QueryParameters.TABLE_NAME,
            TableNameBuilder.extractRawTableName(tableNameWithType));
        List<NameValuePair> parameters = Arrays.asList(enableParallelPushProtectionParameter, tableNameParameter);

        SegmentConversionUtils.uploadSegment(configs, getHttpHeaderForSegment(originalSegmentCRCs[i]), parameters, tableNameWithType, resultSegmentName, uploadURL,
            convertedTarredSegmentFile);
      }

      String outputSegmentNames = segmentConversionResults.stream().map(SegmentConversionResult::getSegmentName)
          .collect(
              Collectors.joining(","));
      postProcess(pinotTaskConfig);
      LOGGER
          .info("Done executing {} on table: {}, input segments: {}, output segments: {}", taskType, tableNameWithType,
              inputSegmentNames, outputSegmentNames);

      return segmentConversionResults;
    } finally {
      FileUtils.deleteQuietly(tempDataDir);
    }
  }

  /**
   * Converts the segment based on the given task config and returns the conversion result.
   */
  private SegmentConversionResult convert(PinotTaskConfig pinotTaskConfig, File indexDir, File workingDir)
      throws Exception {
    Map<String, String> taskConfigs = pinotTaskConfig.getConfigs();
    String tableNameWithType = taskConfigs.get(MinionConstants.TABLE_NAME_KEY);
    TableConfig tableConfig = getTableConfig(tableNameWithType);

    SegmentPurger.RecordPurger recordPurger = new CustomerBasedRetentionPurger(taskConfigs);

    SegmentPurger segmentPurger = new SegmentPurger(indexDir, workingDir, tableConfig, recordPurger, null);
    File purgedSegmentFile = segmentPurger.purgeSegment();
    if (purgedSegmentFile == null) {
      purgedSegmentFile = indexDir;
    }

    return new SegmentConversionResult.Builder().setFile(purgedSegmentFile).setTableNameWithType(tableNameWithType)
        .setSegmentName(taskConfigs.get(MinionConstants.SEGMENT_NAME_KEY))
        .setCustomProperty(RECORD_PURGER_KEY, segmentPurger.getRecordPurger())
        .setCustomProperty(RECORD_MODIFIER_KEY, segmentPurger.getRecordModifier())
        .setCustomProperty(NUM_RECORDS_PURGED_KEY, segmentPurger.getNumRecordsPurged())
        .setCustomProperty(NUM_RECORDS_MODIFIED_KEY, segmentPurger.getNumRecordsModified()).build();
  }

  /**
   * Fetches the CustomerBasedRetentionTask metadata ZNode for the offline table.
   * Checks that the watermarkMs from the ZNode matches the windowStartMs in the task configs.
   * If yes, caches the ZNode version to check during update.
   */
  private void preProcess(PinotTaskConfig pinotTaskConfig)
      throws NoSuchFieldException, IllegalAccessException {
    Map<String, String> configs = pinotTaskConfig.getConfigs();
    String offlineTableName = configs.get(MinionConstants.TABLE_NAME_KEY);
    setPropertyStore();

    ZNRecord customerBasedRetentionTaskZNRecord = MinionTaskMetadataUtils
        .fetchMinionTaskMetadataZNRecord(propertyStore, TASK_TYPE, offlineTableName);
    Preconditions.checkState(customerBasedRetentionTaskZNRecord != null,
        "CustomerBasedRetentionTaskMetadata ZNRecord for table: %s should not be null. Exiting task.",
        offlineTableName);

    CustomerBasedRetentionTaskMetadata customerBasedRetentionTaskMetadata =
        CustomerBasedRetentionTaskMetadata.fromZNRecord(customerBasedRetentionTaskZNRecord);
    long windowStartMs = Long.parseLong(configs.get(WINDOW_START_MS_KEY));
    Preconditions.checkState(customerBasedRetentionTaskMetadata.getWatermarkMs() == windowStartMs,
        "watermarkMs in CustomerBasedRetentionTask metadata: %s does not match windowStartMs: %d in task configs for table: %s. "
            + "ZNode may have been modified by another task", customerBasedRetentionTaskMetadata, windowStartMs,
        offlineTableName);

    _expectedVersion = customerBasedRetentionTaskZNRecord.getVersion();
  }

  private void setPropertyStore()
      throws NoSuchFieldException, IllegalAccessException {
    Field helixManagerField = MinionTaskZkMetadataManager.class.getDeclaredField("_helixManager");
    helixManagerField.setAccessible(true);
    propertyStore = (HelixPropertyStore<ZNRecord>) helixManagerField.get(_minionTaskZkMetadataManager);
  }

  private void postProcess(PinotTaskConfig pinotTaskConfig) {

  }

  private List<Header> getHttpHeaderForSegment(String originalSegmentCrc) {

    // Set original segment CRC into HTTP IF-MATCH header to check whether the original segment get refreshed, so that
    // the newer segment won't get override
    Header ifMatchHeader = new BasicHeader(HttpHeaders.IF_MATCH, originalSegmentCrc);

    // Set segment ZK metadata custom map modifier into HTTP header to modify the segment ZK metadata
    // NOTE: even segment is not changed, still need to upload the segment to update the segment ZK metadata so that
    // segment will not be submitted again
    SegmentZKMetadataCustomMapModifier segmentZKMetadataCustomMapModifier = getSegmentZKMetadataCustomMapModifier();
    Header segmentZKMetadataCustomMapModifierHeader =
        new BasicHeader(FileUploadDownloadClient.CustomHeaders.SEGMENT_ZK_METADATA_CUSTOM_MAP_MODIFIER,
            segmentZKMetadataCustomMapModifier.toJsonString());

    return Arrays.asList(ifMatchHeader, segmentZKMetadataCustomMapModifierHeader);
  }

  private SegmentZKMetadataCustomMapModifier getSegmentZKMetadataCustomMapModifier() {
    return new SegmentZKMetadataCustomMapModifier(SegmentZKMetadataCustomMapModifier.ModifyMode.UPDATE, Collections
        .singletonMap(TASK_TYPE + TASK_TIME_SUFFIX, String.valueOf(System.currentTimeMillis())));
  }
}
