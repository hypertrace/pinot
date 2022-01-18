package org.apache.pinot.minion.tasks.delete;

import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.minion.exception.TaskCancelledException;
import org.apache.pinot.minion.executor.PinotTaskExecutor;

public class CustomDeleteTaskExecutor implements PinotTaskExecutor {
    protected boolean _cancelled = false;

    @Override
    public void cancel() {
        _cancelled = true;
    }

    @Override
    public Boolean executeTask (PinotTaskConfig pinotTaskConfig) {
        // Temporary method (add here)
        do {
            if (_cancelled) {
                throw new TaskCancelledException("Task has been cancelled");
            }
        } while (true);
    }
}
