package org.hypertrace.pinot.plugins;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.pinot.plugin.inputformat.avro.AvroRecordExtractor;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.apache.pinot.spi.data.readers.RecordExtractor;
import org.apache.pinot.spi.plugin.PluginManager;
import org.apache.pinot.spi.stream.StreamMessageDecoder;
import org.hypertrace.core.kafkastreams.framework.serdes.GenericAvroSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
public class GenericAvroMessageDecoder implements StreamMessageDecoder<byte[]> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericAvroMessageDecoder.class);
  GenericAvroSerde genericAvroSerde;
  String topicName;
  private RecordExtractor<Record> _avroRecordExtractor;


  @Override
  public void init(Map<String, String> props, Set<String> fieldsToRead, String topicName)
      throws Exception {
    this.topicName = topicName;

    genericAvroSerde = new GenericAvroSerde();
    genericAvroSerde.configure(props, false);

    String recordExtractorClass = props.get(RECORD_EXTRACTOR_CONFIG_KEY);
    // Backward compatibility to support Avro by default
    if (recordExtractorClass == null) {
      recordExtractorClass = AvroRecordExtractor.class.getName();
    }
    _avroRecordExtractor = PluginManager.get().createInstance(recordExtractorClass);
    _avroRecordExtractor.init(fieldsToRead, null);
  }

  /**
   * {@inheritDoc}
   *
   * <p>NOTE: the payload should contain message content only (without header).
   */
  @Override
  public GenericRow decode(byte[] payload, GenericRow destination) {
    GenericData.Record record = (GenericData.Record) genericAvroSerde.deserializer()
        .deserialize(this.topicName, payload);
    return _avroRecordExtractor.extract(record, destination);
  }

  /**
   * {@inheritDoc}
   *
   * <p>NOTE: the payload should contain message content only (without header).
   */
  @Override
  public GenericRow decode(byte[] payload, int offset, int length, GenericRow destination) {
    return decode(Arrays.copyOfRange(payload, offset, offset + length), destination);
  }
}
