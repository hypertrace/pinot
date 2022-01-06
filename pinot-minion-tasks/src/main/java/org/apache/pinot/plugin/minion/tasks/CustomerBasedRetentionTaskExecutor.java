package org.apache.pinot.plugin.minion.tasks;

import org.apache.pinot.core.minion.PinotTaskConfig;

public class CustomerBasedRetentionTaskExecutor implements PinotTaskExecutor{
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
