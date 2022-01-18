package org.apache.pinot.minion.tasks.delete;

import org.apache.pinot.minion.executor.MinionTaskZkMetadataManager;
import org.apache.pinot.minion.executor.PinotTaskExecutor;
import org.apache.pinot.minion.executor.PinotTaskExecutorFactory;
import org.apache.pinot.spi.annotations.minion.TaskExecutorFactory;

@TaskExecutorFactory
public class CustomDeleteTaskExecutorFactory implements PinotTaskExecutorFactory {

    @Override
    public void init(MinionTaskZkMetadataManager zkMetadataManager) {}

    @Override
    public String getTaskType() {
        return "customDeletionTask";
    }

    @Override
    public PinotTaskExecutor create() {
        return new CustomDeleteTaskExecutor();
    }
}
