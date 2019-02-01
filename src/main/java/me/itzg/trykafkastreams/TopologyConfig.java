package me.itzg.trykafkastreams;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
@Slf4j
public class TopologyConfig {

  private static final String TOPIC_WORDS = "words";
  private static final String TOPIC_UPPER_WORDS = "upper_words";
  private static final String TOPIC_WORD_COUNTS = "word_counts";

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
  public KStream wordsStream(StreamsBuilder streamsBuilder) {
    final KStream<String, String> words = streamsBuilder
        .stream(TOPIC_WORDS, Consumed.with(Serdes.String(), Serdes.String()));

    processToUpper(words);

    processWordCounts(words);

    return words;
  }

  private void processWordCounts(KStream<String, String> words) {
    final KTable<String, Long> wordCount = words
        .peek((key, value) -> {
          log.info("processing value='{}'", value);
        })
        .flatMapValues(value ->
            Arrays.asList(value.toLowerCase().split("\\W+"))
        )
        .peek((key, value) -> {
          log.info("split into value={}", value);
        })
        .groupBy((key, value) -> value)
        .count();

    wordCount.toStream()
        .peek((key, value) -> {
          log.info("counted key={} value={}", key, value);
        })
        // map the count values to a string to make it kafka-console-consumer friendly
        .mapValues(count -> Long.toString(count))
        .to(TOPIC_WORD_COUNTS);
  }

  private void processToUpper(KStream<String, String> words) {
    words
        .mapValues((ValueMapper<String, String>) String::toUpperCase)
        .to(TOPIC_UPPER_WORDS, Produced.valueSerde(Serdes.String()));
  }
}
