package org.apache.pinot.minion.tasks;

public class MinionTaskConstants {
    public static class CusmtomPurgeTask {
        public static final String TASK_TYPE = "CustomPurgeTask";

//         *       "timeWindowStartMs": "",
//                 *       "timeWindowEndMs": "",
//                 *       segmentFilter: "comma-separated-segment-names",
//                *       "maxSegmentsPerIteration": "16",
//                *       "fieldValueFilter.column1": "",
//                *       "fieldValueFilter.column2": "",

        public static final String TIME_WINDOW_START_MS_KEY = "timeWindowStartMs";
        public static final String TIME_WINDOW_END_MS_KEY = "timeWindowEndMs";
        public static final String MAX_SEGMENTS_KEY = "maxSegments";
        public static final String SEGMENT_NAMES_KEY = "segmentNames";
        public static final String FIELD_VALUE_FILTER_PREFIX_KEY = "fieldValueFilter.";
    }
}
