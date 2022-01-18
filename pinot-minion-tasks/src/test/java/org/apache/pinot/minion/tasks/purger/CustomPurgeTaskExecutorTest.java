package org.apache.pinot.minion.tasks.purger;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.helix.AccessOption;
import org.apache.helix.ZNRecord;
import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.pinot.common.utils.config.TableConfigUtils;
import org.apache.pinot.core.common.MinionConstants;
import org.apache.pinot.core.data.readers.GenericRowRecordReader;
import org.apache.pinot.core.data.readers.PinotSegmentRecordReader;
import org.apache.pinot.core.indexsegment.generator.SegmentGeneratorConfig;
import org.apache.pinot.core.minion.PinotTaskConfig;
import org.apache.pinot.core.segment.creator.impl.SegmentIndexCreationDriverImpl;
import org.apache.pinot.minion.MinionContext;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.apache.pinot.spi.data.FieldSpec;
import org.apache.pinot.spi.data.Schema;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.apache.pinot.spi.utils.builder.TableConfigBuilder;
import org.apache.pinot.spi.utils.builder.TableNameBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class CustomPurgeTaskExecutorTest {
  private static final File TEMP_DIR =
      new File(FileUtils.getTempDirectory(), "PurgeTaskExecutorTest");
  private static final File ORIGINAL_SEGMENT_DIR = new File(TEMP_DIR, "originalSegment");
  private static final File PURGED_SEGMENT_DIR = new File(TEMP_DIR, "purgedSegment");

  private static final int NUM_ROWS = 5;
  private static final String TABLE_NAME = "testTable";
  private static final String SEGMENT_NAME = "testSegment";

  private File _originalIndexDir;

  @Before
  public void setUp() throws Exception {
    FileUtils.deleteDirectory(TEMP_DIR);

    TableConfig tableConfig =
        new TableConfigBuilder(TableType.OFFLINE).setTableName(TABLE_NAME).build();
    Schema schema =
        new Schema.SchemaBuilder()
            .addSingleValueDimension("string-column", FieldSpec.DataType.STRING)
            .addSingleValueDimension("int-column", FieldSpec.DataType.INT)
            .addSingleValueDimension("float-column", FieldSpec.DataType.FLOAT)
            .build();

    List<GenericRow> rows = new ArrayList<>(NUM_ROWS);
    for (int i = 1; i <= NUM_ROWS; i++) {
      GenericRow row = new GenericRow();
      row.putValue("string-column", "" + i);
      row.putValue("int-column", Integer.valueOf(i));
      row.putValue("float-column", Float.valueOf(i + 0.1f));
      rows.add(row);
    }
    GenericRowRecordReader genericRowRecordReader = new GenericRowRecordReader(rows);

    SegmentGeneratorConfig config = new SegmentGeneratorConfig(tableConfig, schema);
    config.setOutDir(ORIGINAL_SEGMENT_DIR.getPath());
    config.setSegmentName(SEGMENT_NAME);

    SegmentIndexCreationDriverImpl driver = new SegmentIndexCreationDriverImpl();
    driver.init(config, genericRowRecordReader);
    driver.build();
    _originalIndexDir = new File(ORIGINAL_SEGMENT_DIR, SEGMENT_NAME);

    MinionContext minionContext = MinionContext.getInstance();
    @SuppressWarnings("unchecked")
    ZkHelixPropertyStore<ZNRecord> helixPropertyStore = mock(ZkHelixPropertyStore.class);
    when(helixPropertyStore.get("/CONFIGS/TABLE/testTable_OFFLINE", null, AccessOption.PERSISTENT))
        .thenReturn(TableConfigUtils.toZNRecord(tableConfig));
    minionContext.setHelixPropertyStore(helixPropertyStore);
    minionContext.setRecordModifierFactory(null);
  }

  @Test
  public void testConvert() throws Exception {
    CustomPurgeTaskExecutor purgeTaskExecutor = new CustomPurgeTaskExecutor();

    Map<String, String> taskConfigs = Maps.newHashMap();
    taskConfigs.put(
        MinionConstants.TABLE_NAME_KEY, TableNameBuilder.OFFLINE.tableNameWithType(TABLE_NAME));
    taskConfigs.put("fieldValueFilter.string-column", "1");

    PinotTaskConfig pinotTaskConfig =
        new PinotTaskConfig(MinionConstants.PurgeTask.TASK_TYPE, taskConfigs);
    File purgedIndexDir =
        purgeTaskExecutor.convert(pinotTaskConfig, _originalIndexDir, PURGED_SEGMENT_DIR).getFile();

    try (PinotSegmentRecordReader pinotSegmentRecordReader =
        new PinotSegmentRecordReader(purgedIndexDir)) {
      int numRecordsRemaining = 0;

      GenericRow row = new GenericRow();
      while (pinotSegmentRecordReader.hasNext()) {
        row = pinotSegmentRecordReader.next(row);
        numRecordsRemaining++;
      }

      assertEquals(numRecordsRemaining, NUM_ROWS - 1);
    }
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(TEMP_DIR);
  }
}
