A project that leverages big data and containerization tools to achieve an easy-to-setup big data processing system. Demonstrate uses of the following:
* Spark-Kafka Structured Streaming - a scalable stream processing engine
* Hive - SQL-like interface to enable query on HDFS data
* (Ongoing) Druid - a column-oriented distributed real-time analysis database 
* HBase (Paired with Caffeine Cache) - a non-relational distributed database for quick real-time query
* Docker - an application containerization platform

Command to start the pipeline
```
sh docker/project-runner.sh start [optional job_name] [optional resource_path] [optional kafka_start_time] [optional kafka_end_time]
```
Command to view Kafka data
```
docker container exec -it project_kafka_broker /bin/bash
kafka-console-consumer --bootstrap-server broker:9092 --topic <order | txn | txn_user>
```
Command to stop the pipeline
```
sh docker/project-runner.sh stop
```
