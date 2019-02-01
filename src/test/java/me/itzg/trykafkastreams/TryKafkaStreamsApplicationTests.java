package me.itzg.trykafkastreams;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1,
topics = {
    TopologyConfig.TOPIC_WORDS,
    TopologyConfig.TOPIC_UPPER_WORDS,
    TopologyConfig.TOPIC_WORD_COUNTS
})
public class TryKafkaStreamsApplicationTests {

  static {
    System.setProperty(
        EmbeddedKafkaBroker.BROKER_LIST_PROPERTY,
        "spring.kafka.bootstrap-servers");
  }

  @Test
  public void contextLoads() {
  }

}

