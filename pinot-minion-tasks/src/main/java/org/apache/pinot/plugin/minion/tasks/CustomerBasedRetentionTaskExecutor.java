package org.apache.pinot.plugin.minion.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.minion.executor.BaseMultipleSegmentsConversionExecutor;
import org.apache.pinot.minion.executor.SegmentConversionResult;

public class CustomerBasedRetentionTaskExecutor extends BaseMultipleSegmentsConversionExecutor {

  @Override
  public void preProcess(PinotTaskConfig pinotTaskConfig) {
  }

  @Override
  public void postProcess(PinotTaskConfig pinotTaskConfig) {
  }

  @Override
  protected List<SegmentConversionResult> convert(PinotTaskConfig pinotTaskConfig, List<File> originalIndexDirs, File workingDir)
      throws Exception {
    List<SegmentConversionResult> results = new ArrayList<>();
    return results;
  }
}
