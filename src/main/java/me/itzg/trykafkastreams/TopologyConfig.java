package me.itzg.trykafkastreams;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
@Slf4j
public class TopologyConfig {

  public static final String TOPIC_WORDS = "words";
  public static final String TOPIC_UPPER_WORDS = "upper_words";
  public static final String TOPIC_WORD_COUNTS = "word_counts";
  public static final String STORE_WORD_COUNTS = "word-counts";

  @Bean
  public NewTopic wordsTopic() {
    return new NewTopic(TOPIC_WORDS, 4, (short) 1);
  }

  @Bean
  public NewTopic upperWordsTopic() {
    return new NewTopic(TOPIC_UPPER_WORDS, 4, (short) 1);
  }

  @Bean
  public NewTopic wordCountsTopic() {
    return new NewTopic(TOPIC_WORD_COUNTS, 4, (short) 1);
  }

  @Bean
  public KStream<String,String> wordsStream(StreamsBuilder streamsBuilder) {
    final KStream<String, String> words = streamsBuilder
        .stream(TOPIC_WORDS, Consumed.with(Serdes.String(), Serdes.String()));

    processToUpper(words);

    return words;
  }

  @Bean
  public KTable<String, Long> wordCountTable(KStream<String,String> wordsStream) {
    final Serde<String> stringSerde = Serdes.String();
    return wordsStream
        .peek((key, value) -> {
          log.info("processing value='{}'", value);
        })
        .flatMapValues(value ->
            Arrays.asList(value.toLowerCase().split("\\W+"))
        )
        .peek((key, value) -> {
          log.info("split into value={}", value);
        })
        .groupBy(
            (key, value) -> value,
            Grouped.with("words", stringSerde, stringSerde)
        )
        .count(
            Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(STORE_WORD_COUNTS)
                .withKeySerde(stringSerde)
                .withValueSerde(Serdes.Long())
        );
  }

  @Bean
  public KStream<String, Long> wordCountStream(KTable<String, Long> wordCountTable) {
    final KStream<String, Long> wordCountStream = wordCountTable.toStream();

    wordCountStream
        .peek((key, value) -> {
          log.info("counted key={} value={}", key, value);
        })
        // map the count values to a string to make it kafka-console-consumer friendly
        .mapValues(count -> Long.toString(count))
        .to(
            TOPIC_WORD_COUNTS,
            Produced.with(Serdes.String(), Serdes.String())
        );

    return wordCountStream;
  }

  private void processToUpper(KStream<String, String> words) {
    words
        .mapValues((ValueMapper<String, String>) String::toUpperCase)
        .to(TOPIC_UPPER_WORDS, Produced.valueSerde(Serdes.String()));
  }
}
