package org.apache.pinot.plugin.minion.tasks;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.pinot.common.utils.FileUploadDownloadClient;
import org.apache.pinot.common.utils.TarGzCompressionUtils;
import org.apache.pinot.common.utils.fetcher.SegmentFetcherFactory;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.minion.exception.TaskCancelledException;
import org.apache.pinot.minion.executor.BaseTaskExecutor;
import org.apache.pinot.minion.executor.SegmentConversionResult;
import org.apache.pinot.minion.executor.SegmentConversionUtils;
import org.apache.pinot.spi.utils.builder.TableNameBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerBasedRetentionTaskExecutor extends BaseTaskExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerBasedRetentionTaskExecutor.class);

  public void preProcess(PinotTaskConfig pinotTaskConfig) {

  }

  public void postProcess(PinotTaskConfig pinotTaskConfig) {

  }

  protected List<SegmentConversionResult> convert(PinotTaskConfig pinotTaskConfig, List<File> originalIndexDirs,
      File workingDir) throws Exception {

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
    String uploadURL = configs.get(MinionConstants.UPLOAD_URL_KEY);

    LOGGER.info("Start executing {} on table: {}, input segments: {} with downloadURLs: {}, uploadURL: {}", taskType,
        tableNameWithType, inputSegmentNames, downloadURLString, uploadURL);

    File tempDataDir = new File(new File(MINION_CONTEXT.getDataDir(), taskType), "tmp-" + UUID.randomUUID());
    Preconditions.checkState(tempDataDir.mkdirs());
    String crypterName = getTableConfig(tableNameWithType).getValidationConfig().getCrypterClassName();

    try {
      List<File> inputSegmentFiles = new ArrayList<>();
      for (int i = 0; i < downloadURLs.length; i++) {
        // Download the segment file
        File tarredSegmentFile = new File(tempDataDir, "tarredSegmentFile_" + i);
        LOGGER.info("Downloading segment from {} to {}", downloadURLs[i], tarredSegmentFile.getAbsolutePath());
        SegmentFetcherFactory.fetchAndDecryptSegmentToLocal(downloadURLs[i], tarredSegmentFile, crypterName);

        // Un-tar the segment file
        File segmentDir = new File(tempDataDir, "segmentDir_" + i);
        File indexDir = TarGzCompressionUtils.untar(tarredSegmentFile, segmentDir).get(0);
        inputSegmentFiles.add(indexDir);
      }

      // Convert the segments
      File workingDir = new File(tempDataDir, "workingDir");
      Preconditions.checkState(workingDir.mkdir());
      List<SegmentConversionResult> segmentConversionResults = convert(pinotTaskConfig, inputSegmentFiles, workingDir);

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

        SegmentConversionUtils.uploadSegment(configs, null, parameters, tableNameWithType, resultSegmentName, uploadURL,
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
}
