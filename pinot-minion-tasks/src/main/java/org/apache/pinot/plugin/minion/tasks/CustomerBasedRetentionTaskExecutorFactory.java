package org.apache.pinot.plugin.minion.tasks;

import org.apache.pinot.minion.executor.MinionTaskZkMetadataManager;
import org.apache.pinot.minion.executor.PinotTaskExecutor;
import org.apache.pinot.minion.executor.PinotTaskExecutorFactory;
import org.apache.pinot.spi.annotations.minion.TaskExecutorFactory;

@TaskExecutorFactory
public class CustomerBasedRetentionTaskExecutorFactory implements PinotTaskExecutorFactory {

  @Override
  public void init(MinionTaskZkMetadataManager zkMetadataManager) {}

  @Override
  public String getTaskType() {
    return "customerBasedRetentionTask";
  }

  @Override
  public PinotTaskExecutor create() {
    return new CustomerBasedRetentionTaskExecutor();
  }
}
