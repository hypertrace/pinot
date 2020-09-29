package org.hypertrace.pinot.plugins;

import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.Nullable;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.apache.pinot.spi.data.readers.RecordExtractor;
import org.apache.pinot.spi.data.readers.RecordExtractorConfig;

public class AvroRecordExtractor implements RecordExtractor<GenericRecord> {
  private Set<String> _fields;
  private boolean _extractAll = false;

  @Override
  public void init(Set<String> fields, @Nullable RecordExtractorConfig recordExtractorConfig) {
    _fields = fields;
    if (fields == null || fields.isEmpty()) {
      _extractAll = true;
    }
  }

  @Override
  public GenericRow extract(GenericRecord from, GenericRow to) {
    if (_extractAll) {
      List<Schema.Field> fields = from.getSchema().getFields();
      for (Schema.Field field : fields) {
        String fieldName = field.name();
        to.putValue(fieldName, AvroUtils.convert(from.get(fieldName)));
      }
    } else {
      for (String fieldName : _fields) {
        to.putValue(fieldName, AvroUtils.convert(from.get(fieldName)));
      }
    }
    return to;
  }
}
