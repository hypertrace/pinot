package org.apache.pinot.plugin.minion.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.apache.helix.task.TaskState;
import org.apache.pinot.common.metrics.ControllerGauge;
import org.apache.pinot.controller.helix.core.minion.PinotHelixTaskResourceManager;
import org.apache.pinot.controller.helix.core.minion.PinotTaskManager;
import org.apache.pinot.controller.helix.core.minion.generator.PinotTaskGenerator;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.minion.event.MinionEventObserver;
import org.apache.pinot.minion.event.MinionEventObserverFactory;
import org.apache.pinot.minion.executor.MinionTaskZkMetadataManager;
import org.apache.pinot.spi.config.table.TableTaskConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.apache.pinot.spi.utils.builder.TableConfigBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CustomerBasedRetentionMinionClusterIntegrationTest extends ClusterTest {
  private static final String TASK_TYPE = "TestTask";
  private static final String TABLE_NAME_1 = "testTable1";
  private static final String TABLE_NAME_2 = "testTable2";
  private static final String TABLE_NAME_3 = "testTable3";
  private static final long STATE_TRANSITION_TIMEOUT_MS = 60_000L;  // 1 minute
  private static final int NUM_TASKS = 2;

  private static final AtomicBoolean HOLD = new AtomicBoolean();
  private static final AtomicBoolean TASK_START_NOTIFIED = new AtomicBoolean();
  private static final AtomicBoolean TASK_SUCCESS_NOTIFIED = new AtomicBoolean();
  private static final AtomicBoolean TASK_CANCELLED_NOTIFIED = new AtomicBoolean();
  private static final AtomicBoolean TASK_ERROR_NOTIFIED = new AtomicBoolean();

  private PinotHelixTaskResourceManager _helixTaskResourceManager;
  private PinotTaskManager _taskManager;

  @BeforeClass
  public void setUp()
      throws Exception {
    startZk();
    startController();
    startBroker();
    startServer();

    // Add 3 offline tables, where 2 of them have TestTask enabled
    TableTaskConfig taskConfig = new TableTaskConfig(Collections.singletonMap(TASK_TYPE, Collections.emptyMap()));
    addTableConfig(
        new TableConfigBuilder(TableType.OFFLINE).setTableName(TABLE_NAME_1).setTaskConfig(taskConfig).build());
    addTableConfig(
        new TableConfigBuilder(TableType.OFFLINE).setTableName(TABLE_NAME_2).setTaskConfig(taskConfig).build());
    addTableConfig(new TableConfigBuilder(TableType.OFFLINE).setTableName(TABLE_NAME_3).build());

    _helixTaskResourceManager = _controllerStarter.getHelixTaskResourceManager();
    _taskManager = _controllerStarter.getTaskManager();

    // Register the test task generator into task manager
    PinotTaskGenerator taskGenerator = new CustomerBasedRetentionTaskGenerator();
    taskGenerator.init(_taskManager.getClusterInfoAccessor());
    _taskManager.registerTaskGenerator(taskGenerator);


    startMinion(Collections.singletonList(new CustomerBasedRetentionTaskExecutorFactory()),
        Collections.singletonList(new TestEventObserverFactory()));  }

  @Test
  public void testStopResumeDeleteTaskQueue() {
    // Hold the task
    HOLD.set(true);

    // Should create the task queues and generate a task
    assertNotNull(_taskManager.scheduleTasks().get(TASK_TYPE));
    assertTrue(_helixTaskResourceManager.getTaskQueues()
        .contains(PinotHelixTaskResourceManager.getHelixJobQueueName(TASK_TYPE)));

    // Should generate one more task
    assertNotNull(_taskManager.scheduleTask(TASK_TYPE));

    // Should not generate more tasks
    assertNull(_taskManager.scheduleTasks().get(TASK_TYPE));
    assertNull(_taskManager.scheduleTask(TASK_TYPE));

    // Wait at most 60 seconds for all tasks IN_PROGRESS
    TestUtils.waitForCondition(input -> {
      Collection<TaskState> taskStates = _helixTaskResourceManager.getTaskStates(TASK_TYPE).values();
      assertEquals(taskStates.size(), NUM_TASKS);
      for (TaskState taskState : taskStates) {
        if (taskState != TaskState.IN_PROGRESS) {
          return false;
        }
      }
      assertTrue(TASK_START_NOTIFIED.get());
      assertFalse(TASK_SUCCESS_NOTIFIED.get());
      assertFalse(TASK_CANCELLED_NOTIFIED.get());
      assertFalse(TASK_ERROR_NOTIFIED.get());
      return true;
    }, STATE_TRANSITION_TIMEOUT_MS, "Failed to get all tasks IN_PROGRESS");

    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.IN_PROGRESS, ControllerGauge.TASK_STATUS), NUM_TASKS);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.COMPLETED, ControllerGauge.TASK_STATUS), 0);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.STOPPED, ControllerGauge.TASK_STATUS), 0);

    // Stop the task queue
    _helixTaskResourceManager.stopTaskQueue(TASK_TYPE);

    // Wait at most 60 seconds for all tasks STOPPED
    TestUtils.waitForCondition(input -> {
      Collection<TaskState> taskStates = _helixTaskResourceManager.getTaskStates(TASK_TYPE).values();
      assertEquals(taskStates.size(), NUM_TASKS);
      for (TaskState taskState : taskStates) {
        if (taskState != TaskState.STOPPED) {
          return false;
        }
      }
      assertTrue(TASK_START_NOTIFIED.get());
      assertFalse(TASK_SUCCESS_NOTIFIED.get());
      assertTrue(TASK_CANCELLED_NOTIFIED.get());
      assertFalse(TASK_ERROR_NOTIFIED.get());
      return true;
    }, STATE_TRANSITION_TIMEOUT_MS, "Failed to get all tasks STOPPED");

    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.IN_PROGRESS, ControllerGauge.TASK_STATUS), 0);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.COMPLETED, ControllerGauge.TASK_STATUS), 0);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.STOPPED, ControllerGauge.TASK_STATUS), NUM_TASKS);

    // Resume the task queue, and let the task complete
    _helixTaskResourceManager.resumeTaskQueue(TASK_TYPE);
    HOLD.set(false);

    // Wait at most 60 seconds for all tasks COMPLETED
    TestUtils.waitForCondition(input -> {
      Collection<TaskState> taskStates = _helixTaskResourceManager.getTaskStates(TASK_TYPE).values();
      assertEquals(taskStates.size(), NUM_TASKS);
      for (TaskState taskState : taskStates) {
        if (taskState != TaskState.COMPLETED) {
          return false;
        }
      }
      assertTrue(TASK_START_NOTIFIED.get());
      assertTrue(TASK_SUCCESS_NOTIFIED.get());
      assertTrue(TASK_CANCELLED_NOTIFIED.get());
      assertFalse(TASK_ERROR_NOTIFIED.get());
      return true;
    }, STATE_TRANSITION_TIMEOUT_MS, "Failed to get all tasks COMPLETED");

    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.IN_PROGRESS, ControllerGauge.TASK_STATUS), 0);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.COMPLETED, ControllerGauge.TASK_STATUS), NUM_TASKS);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.STOPPED, ControllerGauge.TASK_STATUS), 0);

    // Delete the task queue
    _helixTaskResourceManager.deleteTaskQueue(TASK_TYPE, false);

    // Wait at most 60 seconds for task queue to be deleted
    TestUtils.waitForCondition(input -> !_helixTaskResourceManager.getTaskTypes().contains(TASK_TYPE),
        STATE_TRANSITION_TIMEOUT_MS, "Failed to delete the task queue");

    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.IN_PROGRESS, ControllerGauge.TASK_STATUS), 0);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.COMPLETED, ControllerGauge.TASK_STATUS), NUM_TASKS);
    assertEquals(_controllerStarter.getControllerMetrics()
        .getValueOfTableGauge(TASK_TYPE + "." + TaskState.STOPPED, ControllerGauge.TASK_STATUS), 0);
  }

  @AfterClass
  public void tearDown()
      throws Exception {
    dropOfflineTable(TABLE_NAME_1);
    dropOfflineTable(TABLE_NAME_2);
    dropOfflineTable(TABLE_NAME_3);
    stopMinion();
    stopServer();
    stopBroker();
    stopController();
    stopZk();
  }

public static class TestEventObserverFactory implements MinionEventObserverFactory {

  @Override
  public void init(MinionTaskZkMetadataManager zkMetadataManager) {
  }

  @Override
  public String getTaskType() {
    return TASK_TYPE;
  }

  @Override
  public MinionEventObserver create() {
    return new MinionEventObserver() {
      @Override
      public void notifyTaskStart(PinotTaskConfig pinotTaskConfig) {
        TASK_START_NOTIFIED.set(true);
      }

      @Override
      public void notifyTaskSuccess(PinotTaskConfig pinotTaskConfig, @Nullable Object executionResult) {
        assertTrue(executionResult instanceof Boolean);
        assertTrue((Boolean) executionResult);
        TASK_SUCCESS_NOTIFIED.set(true);
      }

      @Override
      public void notifyTaskCancelled(PinotTaskConfig pinotTaskConfig) {
        TASK_CANCELLED_NOTIFIED.set(true);
      }

      @Override
      public void notifyTaskError(PinotTaskConfig pinotTaskConfig, Exception exception) {
        TASK_ERROR_NOTIFIED.set(true);
      }
    };
  }
}
}
