package org.apache.pinot.plugin.minion.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pinot.controller.helix.core.minion.ClusterInfoAccessor;
import org.apache.pinot.controller.helix.core.minion.generator.PinotTaskGenerator;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.spi.annotations.minion.TaskGenerator;
import org.apache.pinot.spi.config.table.TableConfig;

@TaskGenerator
public class CustomerBasedRetentionTaskGenerator implements PinotTaskGenerator{

  private ClusterInfoAccessor _clusterInfoAccessor;
  private static final String TASK_TYPE = "customerBasedRetentionTask";

  @Override
  public void init(ClusterInfoAccessor clusterInfoAccessor) {
    _clusterInfoAccessor = clusterInfoAccessor;
  }

  @Override
  public String getTaskType() {
    return TASK_TYPE;
  }

  @Override
  public List<PinotTaskConfig> generateTasks(List<TableConfig> tableConfigs) {
    // Temporary Task (add code here)

    // Generate at most 2 tasks
    if (_clusterInfoAccessor.getTaskStates(TASK_TYPE).size() >= 2) {
      return Collections.emptyList();
    }

    List<PinotTaskConfig> taskConfigs = new ArrayList<>();
    for (TableConfig tableConfig : tableConfigs) {
      Map<String, String> configs = new HashMap<>();
      configs.put("tableName", tableConfig.getTableName());
      configs.put("tableType", tableConfig.getTableType().toString());
      taskConfigs.add(new PinotTaskConfig(TASK_TYPE, configs));
    }
    return taskConfigs;
  }
}
