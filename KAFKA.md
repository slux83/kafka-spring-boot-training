# Starting kafka on docker compose
```
docker-compose --env-file environment.env up
```

It's important to specify the environment file because it has the trick to detect the internal docker image `HOSTNAME`. Sometimes localhost is not even defined, and therefore it's important to specify a valid internal hostname.

# Connect to kafka CLI
Grab the container id by using `docker ps -a`

Connect to the container shell
```
docker exec -it <CONTAINER_ID> bash
```
Access the folder `/opt/bitnami/kafka/bin` and try to crete a topic as a test 
```
/kafka-topics.sh --create --topic test-topic --bootstrap-server 172.17.0.1:9092
```

You can also directly use `exec` via docker-compose
```
docker-compose exec kafka-1 /opt/bitnami/kafka/bin/kafka-topics.sh ...
```

You can also execute the kafka utils from the Linux host if you have kafka installed:
```
slux@slimbook$ ./kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --list --bootstrap-server localhost:9092
```
# Running multiple instances
When you run multiple intances of kadfka, you can connect to any of them. Here's some examples:
```
slux@slimbook:~/.../kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --create --topic test-topic-new --partitions 3 --replication-factor 2 --bootstrap-server localhost:9092
Created topic test-topic-new.
slux@slimbook:~/.../kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --list --bootstrap-server localhost:9094
test-topic
test-topic-new
slux@slimbook:~/.../kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --list --bootstrap-server localhost:9096
test-topic
test-topic-new
slux@slimbook:~/.../kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --create --topic test-topic-new2 --partitions 3 --replication-factor 2 --bootstrap-server localhost:9096
Created topic test-topic-new2.
slux@slimbook:~/.../kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --list --bootstrap-server localhost:9092
test-topic
test-topic-new
test-topic-new2
slux@slimbook:~/.../kafka_2.13-3.8.0/bin$ ./kafka-topics.sh --list --bootstrap-server localhost:9096
test-topic
test-topic-new
test-topic-new2
```

# kafka-console-producer.sh
Script to interact with the broker as producer, e.g., to send a message to a topic
```
./kafka-console-producer.sh --bootstrap-server localhost:9092,localhost:9094 --topic topic-name
```
The script will ask to write text as messages.
You can also set the script to accept a key/value pairs
```
 ./kafka-console-producer.sh --bootstrap-server localhost:9092,localhost:9094 --topic topic-name --property "parse.key=true" --property "key.separator=:"
```
# kafka-console-consumer.sh
Used to consume messages from a topic
```
./kafka-console-consumer.sh --topic topic-name --from-beginning --bootstrap-server localhost:9092,localhost:9094 
Hello World
uhasontuhaos
Yoooooo
Alessio
Fabio
Lollo
```