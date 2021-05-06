package org.apache.pinot.plugin.minion.tasks.purge;

import java.util.List;
import org.apache.pinot.controller.helix.core.minion.ClusterInfoAccessor;
import org.apache.pinot.controller.helix.core.minion.generator.PinotTaskGenerator;
import org.apache.pinot.core.common.MinionConstants.PurgeTask;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.spi.annotations.minion.TaskGenerator;
import org.apache.pinot.spi.config.table.TableConfig;


@TaskGenerator
public class PurgeTaskGenerator implements PinotTaskGenerator {

  @Override
  public void init(ClusterInfoAccessor clusterInfoAccessor) {
  }

  @Override
  public String getTaskType() {
    return PurgeTask.TASK_TYPE;
  }

  @Override
  public List<PinotTaskConfig> generateTasks(List<TableConfig> list) {

    // Record Purger Factory
    return null;
  }
}
