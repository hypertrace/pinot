package org.apache.pinot.minion.tasks.purger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static org.apache.pinot.minion.tasks.MinionTaskConstants.CusmtomPurgeTask.*;

/**
 * Sample configuration <code>
 *     "CustomPurgeTask": {
 *       "timeWindowStartMs": "",
 *       "timeWindowEndMs": "",
 *       segmentFilter: "comma-separated-segment-names",
 *       "maxSegmentsPerIteration": "16",
 *       "fieldValueFilter.column1": "",
 *       "fieldValueFilter.column2": "",
 *     }
 * </code>
 */
public class CustomPurgeTaskConfig {
  private long timeWindowStartMs = 0l;
  private long timeWindowEndMs = Long.MAX_VALUE;
  private int maxSegmentsPerTask = DEFAULT_MAX_SEGMENTS_PER_TASK;
  private List<String> segmentNames = Lists.newArrayList();
  private Map<String, String> fieldValueFilter = Maps.newHashMap();

  public static CustomPurgeTaskConfig fromMap(Map<String, String> configs) {
    CustomPurgeTaskConfig taskConfig = new CustomPurgeTaskConfig();
    if (configs.get(TIME_WINDOW_START_MS_KEY) != null) {
      taskConfig.timeWindowStartMs = Long.valueOf(configs.get(TIME_WINDOW_START_MS_KEY));
    }

      if (configs.get(TIME_WINDOW_END_MS_KEY) != null) {
          taskConfig.timeWindowEndMs = Long.valueOf(configs.get(TIME_WINDOW_END_MS_KEY));
      }

      if (configs.get(MAX_SEGMENTS_PER_TASK_KEY) != null) {
          taskConfig.maxSegmentsPerTask = Integer.valueOf(configs.get(MAX_SEGMENTS_PER_TASK_KEY));
      }

      if (configs.get(SEGMENT_NAMES_KEY) != null) {
          taskConfig.segmentNames = Splitter.on(",").omitEmptyStrings().splitToList(configs.get(SEGMENT_NAMES_KEY));
      }

      if (configs.get(SEGMENT_NAMES_KEY) != null) {
          taskConfig.segmentNames = Splitter.on(",").omitEmptyStrings().splitToList(configs.get(SEGMENT_NAMES_KEY));
      }

    for (Map.Entry<String, String> config : configs.entrySet()) {
      if(config.getKey().startsWith(FIELD_VALUE_FILTER_PREFIX_KEY)) {
        taskConfig.fieldValueFilter.put(config.getKey().substring(FIELD_VALUE_FILTER_PREFIX_KEY.length()), config.getValue());
      }
    }
    return taskConfig;
  }

  public long getTimeWindowStartMs() {
    return timeWindowStartMs;
  }

  public long getTimeWindowEndMs() {
    return timeWindowEndMs;
  }

  public int getMaxSegmentsPerTask() {
    return maxSegmentsPerTask;
  }

  public List<String> getSegmentNames() {
    return segmentNames;
  }

  public Map<String, String> getFieldValueFilter() {
    return fieldValueFilter;
  }

}
