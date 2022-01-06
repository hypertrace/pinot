package org.apache.pinot.plugin.minion.tasks;

import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.minion.executor.PinotTaskExecutor;

public class CustomerBasedRetentionTaskExecutor implements PinotTaskExecutor {
  protected boolean _cancelled = false;

  @Override
  public void cancel() {
    _cancelled = true;
  }

  @Override
  public String executeTask (
      PinotTaskConfig pinotTaskConfig) {
    return "";
  }
}
