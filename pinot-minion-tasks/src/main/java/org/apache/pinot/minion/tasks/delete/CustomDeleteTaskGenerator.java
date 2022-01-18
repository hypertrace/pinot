package org.apache.pinot.minion.tasks.delete;

import org.apache.pinot.controller.helix.core.minion.ClusterInfoAccessor;
import org.apache.pinot.controller.helix.core.minion.generator.PinotTaskGenerator;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.spi.annotations.minion.TaskGenerator;
import org.apache.pinot.spi.config.table.TableConfig;

import java.util.List;

@TaskGenerator
public class CustomDeleteTaskGenerator implements PinotTaskGenerator {
    @Override
    public void init(ClusterInfoAccessor clusterInfoAccessor) {

    }

    @Override
    public String getTaskType() {
        return null;
    }

    @Override
    public List<PinotTaskConfig> generateTasks(List<TableConfig> list) {
        return null;
    }
}
