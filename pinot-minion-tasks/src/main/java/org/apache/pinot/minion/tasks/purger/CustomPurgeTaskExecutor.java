/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.minion.tasks.purger;

import org.apache.pinot.common.metadata.segment.SegmentZKMetadataCustomMapModifier;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.core.minion.SegmentPurger;
import org.apache.pinot.minion.executor.BaseSingleSegmentConversionExecutor;
import org.apache.pinot.minion.executor.SegmentConversionResult;
import org.apache.pinot.spi.config.table.TableConfig;

import java.io.File;
import java.util.Collections;
import java.util.Map;


public class CustomPurgeTaskExecutor extends BaseSingleSegmentConversionExecutor {
  public static final String RECORD_PURGER_KEY = "recordPurger";
  public static final String RECORD_MODIFIER_KEY = "recordModifier";
  public static final String NUM_RECORDS_PURGED_KEY = "numRecordsPurged";
  public static final String NUM_RECORDS_MODIFIED_KEY = "numRecordsModified";


  @Override
  protected SegmentConversionResult convert(PinotTaskConfig pinotTaskConfig, File indexDir, File workingDir)
      throws Exception {
    Map<String, String> taskConfigs = pinotTaskConfig.getConfigs();
    String tableNameWithType = taskConfigs.get(MinionConstants.TABLE_NAME_KEY);

    TableConfig tableConfig = getTableConfig(tableNameWithType);
    CustomPurgeTaskConfig customPurgeTaskConfig = CustomPurgeTaskConfig.fromMap(taskConfigs);
    SegmentPurger.RecordPurger recordPurger = new FieldValueMatchPurger(customPurgeTaskConfig.getFieldValueFilter());

    SegmentPurger segmentPurger = new SegmentPurger(indexDir, workingDir, tableConfig, recordPurger, null);
    File purgedSegmentFile = segmentPurger.purgeSegment();
    if (purgedSegmentFile == null) {
      purgedSegmentFile = indexDir;
    }

    return new SegmentConversionResult.Builder().setFile(purgedSegmentFile).setTableNameWithType(tableNameWithType)
        .setSegmentName(taskConfigs.get(MinionConstants.SEGMENT_NAME_KEY))
        .setCustomProperty(RECORD_PURGER_KEY, segmentPurger.getRecordPurger())
        .setCustomProperty(RECORD_MODIFIER_KEY, segmentPurger.getRecordModifier())
        .setCustomProperty(NUM_RECORDS_PURGED_KEY, segmentPurger.getNumRecordsPurged())
        .setCustomProperty(NUM_RECORDS_MODIFIED_KEY, segmentPurger.getNumRecordsModified()).build();
  }

  @Override
  protected SegmentZKMetadataCustomMapModifier getSegmentZKMetadataCustomMapModifier() {
    return new SegmentZKMetadataCustomMapModifier(SegmentZKMetadataCustomMapModifier.ModifyMode.REPLACE, Collections
        .singletonMap(MinionConstants.PurgeTask.TASK_TYPE + MinionConstants.TASK_TIME_SUFFIX,
            String.valueOf(System.currentTimeMillis())));
  }
}
