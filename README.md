
## Manually testing

Start the Docker composition and then start three sessions:

### Producer

```bash
docker exec -it try-kafka-stream_kafka_1 \
  kafka-console-producer --broker-list localhost:9092 \
  --topic words
```

### Consumer - uppercase

```bash
docker exec -it try-kafka-stream_kafka_1 \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic upper_words
```

### Consumer - word count

```bash
docker exec -it try-kafka-stream_kafka_1 \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic word_counts --property print.key=true
```
