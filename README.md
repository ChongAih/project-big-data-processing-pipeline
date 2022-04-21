A project that leverages big data and containerization tools to achieve an easy-to-setup big data processing system. Demonstrate uses of the following:
* Spark (Structured Streaming) - a scalable stream processing engine
* Flink - a stateful stream processing engine
* Hive - SQL-like interface to enable query on HDFS data
* Kafka - a distributed event streaming platform
* HBase (Paired with Caffeine Cache) - a non-relational distributed database for quick real-time query
* (Ongoing) Druid - a column-oriented distributed real-time analysis database
* Docker - an application containerization platform

Command to start the pipeline
```
sh docker/project-runner.sh start [optional job_name] [optional resource_path] [optional kafka_start_time] [optional kafka_end_time]
```
Command to view Kafka data
```
docker container exec -it project_kafka_broker /bin/bash
kafka-console-consumer --bootstrap-server broker:9092 --topic <order | txn | txn_user>

docker container exec -it project_kafka_broker bash -c "kafka-console-consumer --bootstrap-server broker:9092 --topic txn"
```
Command to stop the pipeline
```
sh docker/project-runner.sh stop
```
User Interface
* Spark Master - http://localhost:8080
* Spark Worker - http://localhost:8081

