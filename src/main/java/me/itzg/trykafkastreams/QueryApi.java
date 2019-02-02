package me.itzg.trykafkastreams;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/query")
public class QueryApi {

  private final StreamsBuilderFactoryBean streamsBuilderFactory;

  @Autowired
  public QueryApi(StreamsBuilderFactoryBean streamsBuilderFactory) {
    this.streamsBuilderFactory = streamsBuilderFactory;
  }

  @GetMapping("wordCounts")
  public List<WordCount> getAllWordCounts() {

    final ReadOnlyKeyValueStore<String, Long> kvStore = streamsBuilderFactory.getKafkaStreams()
        .store(
            TopologyConfig.STORE_WORD_COUNTS,
            QueryableStoreTypes.keyValueStore()
        );

    final List<WordCount> result = new ArrayList<>();
    final KeyValueIterator<String, Long> all = kvStore.all();
    while (all.hasNext()) {
      final KeyValue<String, Long> entry = all.next();
      result.add(
          WordCount.builder()
              .word(entry.key)
              .count(entry.value)
              .build()
      );
    }

    return result;
  }
}
