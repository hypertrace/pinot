package org.apache.pinot.plugin.minion.tasks;

import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.COLUMNS_TO_CONVERT_KEY;
import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.CUSTOMER_RETENTION_CONFIG;
import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.RETENTION_PERIOD_KEY;
import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.TASK_TYPE;
import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.WINDOW_END_MS_KEY;
import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.WINDOW_START_MS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.helix.task.TaskState;
import org.apache.pinot.common.metadata.segment.OfflineSegmentZKMetadata;
import org.apache.pinot.common.minion.RealtimeToOfflineSegmentsTaskMetadata;
import org.apache.pinot.controller.helix.core.minion.ClusterInfoAccessor;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.TableTaskConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.apache.pinot.spi.utils.builder.TableConfigBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CustomerBasedRetentionTaskGeneratorTest {

  private static final String RAW_TABLE_NAME = "testTable";
  private static final String OFFLINE_TABLE_NAME = "testTable_OFFLINE";

  @Test
  public void testGenerateTaskConfigs() {
    ClusterInfoAccessor mockClusterInfoProvide = mock(ClusterInfoAccessor.class);

    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGenerator = new CustomerBasedRetentionTaskGenerator();
    customerBasedRetentionTaskGenerator.init(mockClusterInfoProvide);

    // Skip task generation, if realtime table
    TableConfig realtimeTableConfig = new TableConfigBuilder(
        TableType.REALTIME).setTableName(RAW_TABLE_NAME).build();
    List<PinotTaskConfig> pinotTaskConfigs = customerBasedRetentionTaskGenerator.generateTasks(
        Lists.newArrayList(realtimeTableConfig));
    assertTrue(pinotTaskConfigs.isEmpty());

    // No tableTaskConfig, error
    TableConfig offlineTableConfig = getOfflineTableConfig(new HashMap<>());
    offlineTableConfig.setTaskConfig(null);
    try{
      customerBasedRetentionTaskGenerator.generateTasks(Lists.newArrayList(offlineTableConfig));
      fail("Should have failed for null tableTaskConfig");
    } catch (NullPointerException e){}

    // No taskConfig for task, error
    offlineTableConfig = getOfflineTableConfig(new HashMap<>());
    try{
      customerBasedRetentionTaskGenerator.generateTasks(Lists.newArrayList(offlineTableConfig));
      fail("Should have failed for null tableTaskConfig");
    } catch (NullPointerException e){}
  }

  @Test
  public void testGenerateTasksEmptyTableConfig() {
    ClusterInfoAccessor mockClusterInfoProvide = mock(ClusterInfoAccessor.class);
    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGenerator = new CustomerBasedRetentionTaskGenerator();
    customerBasedRetentionTaskGenerator.init(mockClusterInfoProvide);
    assertEquals(List.of(), customerBasedRetentionTaskGenerator.generateTasks(List.of()));
  }

  @Test
  public void testGenerateTasksSimultaneousConstraints() {
    Map<String, Map<String, String>> taskConfigsMap = new HashMap<>();
    taskConfigsMap.put(TASK_TYPE, new HashMap<>());
    TableConfig offlineTableConfig = getOfflineTableConfig(taskConfigsMap);

    ClusterInfoAccessor mockClusterInfoProvide = mock(ClusterInfoAccessor.class);
    Map<String, TaskState> taskStatesMap = new HashMap<>();
    String taskName = "Task_CustomerBasedRetentionTask_" + System.currentTimeMillis();
    Map<String, String> taskConfigs = new HashMap<>();
    taskConfigs.put(MinionConstants.TABLE_NAME_KEY, OFFLINE_TABLE_NAME);

    when(mockClusterInfoProvide.getTaskStates(TASK_TYPE)).thenReturn(taskStatesMap);
    when(mockClusterInfoProvide.getTaskConfigs(taskName))
        .thenReturn(Lists.newArrayList(new PinotTaskConfig(TASK_TYPE, taskConfigs)));
    when(mockClusterInfoProvide.getMinionRealtimeToOfflineSegmentsTaskMetadata(OFFLINE_TABLE_NAME))
        .thenReturn(new RealtimeToOfflineSegmentsTaskMetadata(OFFLINE_TABLE_NAME, 100_000L));

    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGenerator = new CustomerBasedRetentionTaskGenerator();
    customerBasedRetentionTaskGenerator.init(mockClusterInfoProvide);

    // if same task and table, IN_PROGRESS, then don't generate again
    taskStatesMap.put(taskName, TaskState.IN_PROGRESS);
    List<PinotTaskConfig> pinotTaskConfigs = customerBasedRetentionTaskGenerator.generateTasks(Lists.newArrayList(offlineTableConfig));
    assertTrue(pinotTaskConfigs.isEmpty());
  }

  @Test
  public void testGenerateTasksNoSegments() throws NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> taskConfigsMap = new HashMap<>();
    taskConfigsMap.put(TASK_TYPE, new HashMap<>());
    TableConfig offlineTableConfig = getOfflineTableConfig(taskConfigsMap);

    ClusterInfoAccessor mockClusterInfoProvide = mock(ClusterInfoAccessor.class);
    when(mockClusterInfoProvide.getTaskStates(TASK_TYPE)).thenReturn(new HashMap<>());

    // No segments in table
    when(mockClusterInfoProvide.getOfflineSegmentsMetadata(OFFLINE_TABLE_NAME)).thenReturn(Lists.newArrayList());

    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGenerator = new CustomerBasedRetentionTaskGenerator();
    customerBasedRetentionTaskGenerator.init(mockClusterInfoProvide);

    // mock watermark
    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGeneratorSpy = Mockito.spy(customerBasedRetentionTaskGenerator);
    Mockito.doReturn(0L).when(customerBasedRetentionTaskGeneratorSpy).getWatermarkMs(Mockito.any(),Mockito.any(),Mockito.any());

    List<PinotTaskConfig> pinotTaskConfigs = customerBasedRetentionTaskGeneratorSpy.generateTasks(Lists.newArrayList(offlineTableConfig));
    assertTrue(pinotTaskConfigs.isEmpty());
  }

  @Test
  public void testGenerateTasksWithConvertedSegments() throws NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> taskConfigsMap = new HashMap<>();
    taskConfigsMap.put(TASK_TYPE, new HashMap<>());
    TableConfig offlineTableConfig = getOfflineTableConfig(taskConfigsMap);

    ClusterInfoAccessor mockClusterInfoProvide = mock(ClusterInfoAccessor.class);
    when(mockClusterInfoProvide.getTaskStates(TASK_TYPE)).thenReturn(new HashMap<>());

    // Add segments
    Map<String, String> customMap = new HashMap<>();
    customMap.put(COLUMNS_TO_CONVERT_KEY + MinionConstants.TASK_TIME_SUFFIX,"");
    OfflineSegmentZKMetadata seg1 = getOfflineSegmentZKMetadata("testTable__0__0__12345", "download1", customMap, 1L,1L);
    OfflineSegmentZKMetadata seg2 = getOfflineSegmentZKMetadata("testTable__1__0__12345", "download2", customMap, 2L,2L);
    when(mockClusterInfoProvide.getOfflineSegmentsMetadata(OFFLINE_TABLE_NAME)).thenReturn(Lists.newArrayList(seg1, seg2));

    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGenerator = new CustomerBasedRetentionTaskGenerator();
    customerBasedRetentionTaskGenerator.init(mockClusterInfoProvide);

    // mock watermark
    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGeneratorSpy = Mockito.spy(customerBasedRetentionTaskGenerator);
    Mockito.doReturn(0L).when(customerBasedRetentionTaskGeneratorSpy).getWatermarkMs(Mockito.any(),Mockito.any(),Mockito.any());

    List<PinotTaskConfig> pinotTaskConfigs = customerBasedRetentionTaskGeneratorSpy.generateTasks(Lists.newArrayList(offlineTableConfig));
    assertTrue(pinotTaskConfigs.isEmpty());
  }

  @Test
  public void testGenerateTasksWithUnconvertedSegments() throws NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> taskConfigsMap = new HashMap<>();
    taskConfigsMap.put(TASK_TYPE, new HashMap<>());
    TableConfig offlineTableConfig = getOfflineTableConfig(taskConfigsMap);

    ClusterInfoAccessor mockClusterInfoProvide = mock(ClusterInfoAccessor.class);
    when(mockClusterInfoProvide.getTaskStates(TASK_TYPE)).thenReturn(new HashMap<>());

    // Add segments
    OfflineSegmentZKMetadata seg1 = getOfflineSegmentZKMetadata("testTable__0__0__12345", "download1", new HashMap<>(), 1L,1L);
    OfflineSegmentZKMetadata seg2 = getOfflineSegmentZKMetadata("testTable__1__0__12345", "download2",new HashMap<>(), 2L,2L);
    when(mockClusterInfoProvide.getOfflineSegmentsMetadata(OFFLINE_TABLE_NAME)).thenReturn(Lists.newArrayList(seg2, seg1)); // to test sorting behaviour

    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGenerator = new CustomerBasedRetentionTaskGenerator();
    customerBasedRetentionTaskGenerator.init(mockClusterInfoProvide);

    // mock watermark
    CustomerBasedRetentionTaskGenerator customerBasedRetentionTaskGeneratorSpy = Mockito.spy(customerBasedRetentionTaskGenerator);
    Mockito.doReturn(0L).when(customerBasedRetentionTaskGeneratorSpy).getWatermarkMs(Mockito.any(),Mockito.any(),Mockito.any());

    List<PinotTaskConfig> pinotTaskConfigs = customerBasedRetentionTaskGeneratorSpy.generateTasks(Lists.newArrayList(offlineTableConfig));
    assertEquals(pinotTaskConfigs.size(), 1);
    assertEquals(pinotTaskConfigs.get(0).getTaskType(), TASK_TYPE);

    Map<String,String > configs = pinotTaskConfigs.get(0).getConfigs();
    assertEquals(configs.size(), 9);
    assertEquals(configs.get(MinionConstants.TABLE_NAME_KEY),OFFLINE_TABLE_NAME);
    assertEquals(configs.get(MinionConstants.SEGMENT_NAME_KEY),"testTable__0__0__12345,testTable__1__0__12345");
    assertEquals(configs.get(MinionConstants.DOWNLOAD_URL_KEY),"download1,download2");
    assertEquals(configs.get(MinionConstants.ORIGINAL_SEGMENT_CRC_KEY),"1,2");
    assertEquals(configs.get(CUSTOMER_RETENTION_CONFIG),"{customer_1=1h}");
    assertEquals(configs.get(RETENTION_PERIOD_KEY),"1h");
    assertEquals(configs.get(WINDOW_START_MS_KEY),"0");
    assertEquals(configs.get(WINDOW_END_MS_KEY),"9223372036854775807");
  }

  @Test
  public void testGenerateTasksWithUnconvertedSegmentsWithMultipleRetentionPeriods() {

  }

  @Test
  public void testGetWatermark() {

  }

  private TableConfig getOfflineTableConfig(Map<String, Map<String, String>> taskConfigsMap) {
    return new TableConfigBuilder(TableType.OFFLINE).setTableName(RAW_TABLE_NAME).setTaskConfig(new TableTaskConfig(taskConfigsMap)).build();
  }

  private OfflineSegmentZKMetadata getOfflineSegmentZKMetadata(String segmentName, String downloadUrl, Map<String,String> customMap, long crc, long startTime) {
    OfflineSegmentZKMetadata offlineSegmentZKMetadata = new OfflineSegmentZKMetadata();
    offlineSegmentZKMetadata.setSegmentName(segmentName);
    offlineSegmentZKMetadata.setDownloadUrl(downloadUrl);
    offlineSegmentZKMetadata.setCustomMap(customMap);
    offlineSegmentZKMetadata.setCrc(crc);
    offlineSegmentZKMetadata.setStartTime(startTime);
    return offlineSegmentZKMetadata;
  }
}
