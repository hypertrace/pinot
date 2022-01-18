package org.apache.pinot.minion.tasks;

public class MinionTaskConstants {
    public static class CusmtomPurgeTask {
        public static final String TASK_TYPE = "CustomPurgeTask";
        public static final String TIME_WINDOW_START_MS_KEY = "windowStartMs";
        public static final String TIME_WINDOW_END_MS_KEY = "windowEndMs";
        public static final String MAX_SEGMENTS_PER_TASK_KEY = "maxSegments";
        public static final Integer DEFAULT_MAX_SEGMENTS_PER_TASK = 16;
        public static final String SEGMENT_NAMES_KEY = "segmentNames";
        public static final String FIELD_VALUE_FILTER_PREFIX_KEY = "fieldValueFilter.";
    }
}
