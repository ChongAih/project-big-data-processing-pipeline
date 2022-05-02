## Data Processing Pipeline 

A project that leverages big data and containerization tools to achieve an easy-to-setup big data processing system. Demonstrate uses of the following:
* Spark (Structured Streaming) - a scalable stream processing engine
* Flink - a stateful stream processing engine
* Hive - SQL-like interface to enable query on HDFS data
* Kafka - a distributed event streaming platform
* HBase (Paired with Caffeine Cache) - a non-relational distributed database for quick real-time query
* Druid - a column-oriented distributed real-time analysis database
* Docker - an application containerization platform

### 1. Change directory
```
cd docker
```

### 2. Command to start the pipeline
```
sh project-runner.sh <start | stop> [optional processing_platform] [optional job_name] [optional resource_path] [optional kafka_start_time] [optional kafka_end_time] |
```
* <start | stop> - start or stop all docker container
* <processing_platform> - flink or spark. e.g, flink/spark. Default is set to be 'spark'
* <job_name> - optional processing job class name. e.g, Txn/TxnUser. Default is set to be 'Txn'
* <resource_path> optional job configuration file name. e.g, txn.conf/txn_user.conf. Default is set to be txn.conf
* <kafka_start_time> optional Kafka consumption start time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1
* <kafka_end_time> optional Kafka consumption end time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1

##### 2.1. Command to start the Spark Txn pipeline
```
sh project-runner.sh start spark Txn txn.conf 
```

##### 2.2. Command to start the Spark TxnUser pipeline
```
sh project-runner.sh start spark TxnUser txn_user.conf
```

##### 2.3. Command to start the Flink Txn pipeline
```
sh project-runner.sh start flink FlinkTxn flink_txn.conf
```

Note that there is a known bug in Kafka 2.3.0 when the checkpoint mechanism is enabled in the Flink processing pipeline - 
the Flink job throws exception 'org.apache.kafka.common.errors.UnknownProducerIdException' when a streams application has
little traffic. Reader may refer to the questions posted on [StackOverflow](https://stackoverflow.com/questions/51036351/kafka-unknown-producer-id-exception)

### 3. Command to view Kafka data
```
docker container exec -it project_kafka_broker /bin/bash
kafka-console-consumer --bootstrap-server broker:9092 --topic <order | txn | txn_user>

docker container exec -it project_kafka_broker bash -c "kafka-console-consumer --bootstrap-server broker:9092 --topic txn"
```

### 4. Command to read from Druid datasource
Two ways to get the metrics from the Druid datasource:
* Submit native query via curl
    ```
    DRUID_QUERY_JSON="./druid_txn_query.json"
    
    DRUID_QUERY_JSON="./druid_txn_user_query.json"
    ```
    ```
    query=$(cat $DRUID_QUERY_JSON | sed "s/start_time/$(date +"%Y-%m-%d")T00:00:00+08:00/" | sed "s/end_time/$(date -v +1d +"%Y-%m-%d")T00:00:00+08:00/")
    
    curl -X POST \
      http://localhost:8889/druid/v2/?pretty \
      -H 'cache-control: no-cache' \
      -H 'content-type: application/json' \
      -d "$query"
    ```
* Submit SQL query via [Druid UI](http://localhost:8889)
    ```
    SELECT 
      country, SUM("gmv") AS gmv_sum, SUM("gmv_usd") AS gmv_usd_sum, SUM("count") AS order_count
    FROM txn
    GROUP BY country
    
    
    SELECT
    country, SUM("count") AS user_count
    FROM txn_user
    GROUP BY country
    ```
    ![Txn query](images/Druid_UI_SQL_txn_query.png?)
    ![Txn_user query](images/Druid_UI_SQL_txn_user_query.png?)

### 5. Command to stop the pipeline
```
sh project-runner.sh stop
```

### 6. User Interface
* Spark Master - http://localhost:8080
* Spark Worker - http://localhost:8081
* Flink Job Manager - http://localhost:8083
* HBase Master - http://localhost:16010
* Druid Router (For manual ingestion & SQL query) - http://localhost:8889
* Druid Overload - http://localhost:8086

### 7. Dependencies
* Docker (20.10.7) - https://docs.docker.com/get-docker/
* Java (1.8)
* Scala (2.11.12)
* Mvn (10.15.7)
* Flink (1.14.4)
* Kafka (2.3.0)
* Spark (2.4.5)