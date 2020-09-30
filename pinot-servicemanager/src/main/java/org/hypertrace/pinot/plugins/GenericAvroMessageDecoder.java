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
    try {
      LOGGER.info(String.format("initializing GenericAvroMessageDecoder for topic:%s", topicName));
      this.topicName = topicName;

      genericAvroSerde = new GenericAvroSerde();
      LOGGER.info("GenericAvroMessageDecoder reached 1");
      genericAvroSerde.configure(props, false);

      LOGGER.info("GenericAvroMessageDecoder reached 2");

      String recordExtractorClass = null;
      if (props != null) {
        recordExtractorClass = props.get(RECORD_EXTRACTOR_CONFIG_KEY);
      }
      // Backward compatibility to support Avro by default
      if (recordExtractorClass == null) {
        recordExtractorClass = AvroRecordExtractor.class.getName();
      }
      LOGGER.info("GenericAvroMessageDecoder reached 3");
      _avroRecordExtractor = PluginManager.get().createInstance(recordExtractorClass);
      _avroRecordExtractor.init(fieldsToRead, null);
      LOGGER.info(String
          .format("Successfully initialized GenericAvroMessageDecoder for topic:%s", topicName));
    } catch (Exception e) {
      LOGGER.info("Failed in init GenericAvroMessageDecoder", e);
      throw e;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>NOTE: the payload should contain message content only (without header).
   */
  @Override
  public GenericRow decode(byte[] payload, GenericRow destination) {
    try {
      GenericData.Record record = (GenericData.Record) genericAvroSerde.deserializer()
          .deserialize(this.topicName, payload);
      return _avroRecordExtractor.extract(record, destination);
    } catch (Exception e) {
      LOGGER.info("Failed in decode GenericAvroMessageDecoder", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>NOTE: the payload should contain message content only (without header).
   */
  @Override
  public GenericRow decode(byte[] payload, int offset, int length, GenericRow destination) {
    try {
      return decode(Arrays.copyOfRange(payload, offset, offset + length), destination);
    } catch (Exception e) {
      LOGGER.info("Failed in decode1 GenericAvroMessageDecoder", e);
      throw new RuntimeException(e);
    }
  }
}
