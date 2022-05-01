### Requirement
* Docker (20.10.7)
* Java (1.8)
* Scala (2.11.12)
* Mvn (10.15.7)
* Flink (1.14.4)
* Kafka (2.3.0)
* Spark (2.4.5)

### Docker

https://stackoverflow.com/questions/44084846/cannot-connect-to-the-docker-daemon-on-macos
https://www.cprime.com/resources/blog/docker-on-mac-with-homebrew-a-step-by-step-tutorial/
https://vsupalov.com/docker-build-time-env-values/#:~:text=While%20you%20can't%20directly,right%20into%20your%20ENV%20instructions.&text=You%20can%20change%20the%20value,build%20without%20editing%20the%20Dockerfile.

- alias docker="/usr/local/Cellar/docker/20.10.7/bin/docker"
- open the docker apps (ui) to start docker daemon
- cd /Users/chongaih.hau/Desktop/javascript_playground/docker && docker build -t nodeapp . --> nodeapp is the tag name
- docker run --name nodeapp -p 9999:9999 nodeapp --> nodeapp is the tag name
  http://localhost:9999/, http://localhost:9999/app1, http://localhost:9999/app2, http://localhost:9999/admin
- docker ps --> show all containers
- docker stop <container id>
- kafka broker docker
    - use this create
      topic: https://github.com/confluentinc/examples/blob/5.1.1-post/microservices-orders/docker-compose.yml#L182-L215
    - x single and mutltinode
        - https://www.baeldung.com/ops/kafka-docker-setup
        - https://www.baeldung.com/kafka-docker-connection
    - official doc: https://docs.confluent.io/5.2.0/quickstart/cos-docker-quickstart.html
    - https://hub.docker.com/r/confluentinc/cp-kafka
    - command: (remember to go to docker folder)
        - docker-compose -f docker-compose.yml up -d
        - docker-compose down -v
    - functionality check command:
        - From inside of container: docker container exec -it project_kafka_broker /bin/bash
        - kafka-topics --list --zookeeper zookeeper:2181
        - kafka-console-producer --broker-list broker:9092 --topic order
        - kafka-console-consumer --bootstrap-server broker:9092 --topic order
        - From outside of container - cd /Users/chongaih.hau/kafka_2.13-2.6.0 && bin/kafka-console-producer.sh
          --bootstrap-server localhost:29092 --topic order
        - cd /Users/chongaih.hau/kafka_2.13-2.6.0 && bin/kafka-console-consumer.sh --bootstrap-server localhost:29092
          --topic order
    - flink cant write/ checkpoint due to isr https://github.com/wurstmeister/kafka-docker/issues/218    
      https://stackoverflow.com/questions/68757649/kafka-transactional-producer-throws-timeout-expired-while-initializing-transact
- python-conda:
    - environment.yml set channels to download package
    - docker container exec -it project_conda_python /bin/bash
- spark:
    - https://dev.to/mvillarrealb/creating-a-spark-standalone-cluster-with-docker-and-docker-compose-2021-update-6l4
    - https://hub.docker.com/r/bitnami/spark/
    - https://stackoverflow.com/questions/61051316/too-large-frame-error-when-running-spark-shell-on-standalone-cluster
    - https://issueexplorer.com/issue/bitnami/bitnami-docker-spark/46
    - https://stackoverflow.com/questions/46159300/nosuch-file-exception-spark-standalone-cluster
    - error in running --packages in
      bitnami https://stackoverflow.com/questions/60630832/i-cannot-use-package-option-on-bitnami-spark-docker-container
    - error in spark submit
      https://github.com/bitnami/charts/issues/2883
    - docker container exec -it project_spark_submit /bin/bash
    - /opt/bitnami/spark/work --> application jar is copied here and driver will run the application, driver stderr/
      stdout is written here
    - bitnami spark does not allow any write on /opt/bitnami/spark is not writable
- hbase:
    - access docker hbase from host: https://github.com/big-data-europe/docker-hbase/issues/11
    - https://www.findbestopensource.com/product/dajobe-hbase-docker
- checkpoint:
  - docker container exec -it docker_flink-jobmanager_1 /bin/bash --> cd /tmp && ls checkpoint 
  - docker container exec -it project_spark_worker /bin/bash --> cd /tmp && ls checkpoint
- druid:
    - https://github.com/apache/druid/tree/0.22.1/distribution/docker
    - https://stackoverflow.com/questions/61153689/org-apache-druid-java-util-common-ise-no-default-server-found
- docker container with same container port:
    - https://stackoverflow.com/questions/55192991/multiple-docker-containers-with-same-container-port-connected-to-the-same-networ#:~:text=compose%20created%20network.-,Ports%20must%20be%20unique%20per%20host%20but%20each%20service%20in,name%3E%3A%20.
- zombie:
    - https://stackoverflow.com/questions/49162358/docker-init-zombies-why-does-it-matter

### Spark

- Encoder: https://jaceklaskowski.gitbooks.io/mastering-spark-sql/content/spark-sql-Encoder.html
- Structured streaming:
  * https://aseigneurin.github.io/2018/08/14/kafka-tutorial-8-spark-structured-streaming.html
  * https://spark.apache.org/docs/latest/structured-streaming-kafka-integration.html
- https://sparkbyexamples.com/spark/convert-case-class-to-spark-schema/
- https://github.com/holdenk/learning-spark-examples/blob/master/src/main/scala/com/oreilly/learningsparkexamples/scala/BasicParseJsonWithJackson.scala
- https://stackoverflow.com/questions/35485662/local-class-incompatible-exception-when-running-spark-standalone-from-ide
- https://stackoverflow.com/questions/40015777/how-to-perform-one-operation-on-each-executor-once-in-spark
- https://stackoverflow.com/questions/50933289/why-is-my-initialized-object-not-being-passed-to-my-spark-tasks
- https://stackoverflow.com/questions/40015777/how-to-perform-one-operation-on-each-executor-once-in-spark
- https://medium.com/quantumblack/spark-udf-deep-insights-in-performance-f0a95a4d8c62
- Checkpoint:
    * https://spark.apache.org/docs/latest/structured-streaming-kafka-integration.html
      
        Structured Streaming manages which offsets are consumed internally, rather than rely on the kafka Consumer to do it. This will ensure that no data is missed when new topics/partitions are dynamically subscribed. Note that startingOffsets only applies when a new streaming query is started, and that resuming will always pick up from where the query left off. Note that when the offsets consumed by a streaming application no longer exist in Kafka (e.g., topics are deleted, offsets are out of range, or offsets are removed after retention period), the offsets will not be reset and the streaming application will see data loss. In extreme cases, for example the throughput of the streaming application cannot catch up the retention speed of Kafka, the input rows of a batch might be gradually reduced until zero when the offset ranges of the batch are completely not in Kafka. Enabling failOnDataLoss option can ask Structured Streaming to fail the query for such cases.
    * if reuse the same checkpoint path the job will start from the offset it last stops
    ```
    22/04/23 08:09:16 INFO MicroBatchExecution: Starting [id = 1d9cf6ae-5c07-463a-80ed-1de555820f14, runId = eddffaff-08a0-4ce6-9e65-9e3d1dedb700]. 
    Use file:///tmp/checkpoint/txn to store the query checkpoint.)
    (Resuming at batch 20 with committed offsets {KafkaV2[Subscribe[order]]: {"order":{"0":1153}}} and available offsets {KafkaV2[Subscribe[order]]: {"order":{"0":1153}}})
    ```
- Command: https://sparkbyexamples.com/spark/spark-submit-command/
- If your files are available via http, hdfs, etc. you should be able to use addFile and --files in client as well 
  as in cluster mode. In cluster mode, a local file, which has not been added to the spark-submit will not be found 
  via addFile. This is because the driver (application master) is started on the cluster and is already running when he 
  reaches the addFile call. It is to late at this point. The application has already been submited, and the local file 
  system is the file system of a specific cluster node.
  
  https://stackoverflow.com/questions/38879478/sparkcontext-addfile-vs-spark-submit-files

  submit a zip file
  1. if file at hdfs:
        * no need to use --py-files on spark-submit
        * client mode - initialize spark and call sparkContext.addPyFile("hdfs/path/to/zip/or/file")
        * cluster mode - initialize spark and call sparkContext.addPyFile("hdfs/path/to/zip/or/file")
  2. if file at local:
        * client mode
            * no need to initialize spark and can use --py-files <zip/file> on spark-submit
            * need to initialize spark and call sparkContext.addPyFile("zip/or/file")
        * cluster mode:
            * in cluster mode, spark needs to be initialized
            * call sparkContext.addPyFile("hdfs/path/to/zip/or/file")

  https://stackoverflow.com/questions/36461054/i-cant-seem-to-get-py-files-on-spark-to-work

  https://janetvn.medium.com/how-to-add-multiple-python-custom-modules-to-spark-job-6a8b943cdbbc

  --jars vs SparkContext.addJar: These are identical. Only one is set through Spark submit and one via code. Choose the 
  one which suits you better. One important thing to note is that using either of these options does not add the JAR
  file to your driver/executor classpath. You'll need to explicitly add them using the extraClassPath configuration on both.

  https://www.hadoopinrealworld.com/how-to-properly-add-jars-to-a-spark-application/
  
  https://stackoverflow.com/questions/37132559/add-jars-to-a-spark-job-spark-submit
  
  https://stackoverflow.com/questions/43257324/what-is-the-use-of-driver-class-path-in-the-spark-command

### Maven
- multimodule - https://mkyong.com/maven/maven-how-to-create-a-multi-module-project/
  
### HBase
- https://dwgeek.com/how-to-execute-hbase-commands-from-shell-script-examples.html/
- https://www.tutorialspoint.com/hbase/hbase_create_table.htm
- https://blog.cloudera.com/what-are-hbase-znodes/

### Caffeine
- https://self-learning-java-tutorial.blogspot.com/2021/04/caffeine-time-based-eviction.html

### Nginx
- location priority - https://stackoverflow.com/questions/5238377/nginx-location-priority

### Bash

https://askubuntu.com/questions/53177/bash-script-to-set-environment-variables-not-working

### ConfigFactory

https://dzone.com/articles/typesafe-config-features-and-example-usage

### TypeTag case match generic type

https://gist.github.com/jkpl/5279ee05cca8cc1ec452fc26ace5b68b

### Jackson JSON

https://www.baeldung.com/jackson-object-mapper-tutorial

### Kafka

https://gankrin.org/how-to-setup-multi-node-kafka-cluster-or-brokers/
https://www.baeldung.com/ops/kafka-docker-setup

### Git

https://stackoverflow.com/questions/18935539/authenticate-with-github-using-a-token

### Flink

streamenv
* config - backend state - https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/ops/state/state_backends/
* config - checkpoint - https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/checkpointing/
* config - execution - time characteristics - https://zhuanlan.zhihu.com/p/344540564
* config - restart-strategy - https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/execution/task_failure_recovery/
* parallelism vs maxparallism - parallelism is for all operator while max parallelism is for key state, max parallelism must be > parallelism for key state operation to work
  https://stackoverflow.com/questions/54561716/apache-flink-what-is-the-difference-of-setparallelism-and-setmaxparallelism#:~:text=Flink%20provides%20two%20settings%3A,effective%20parallelism%20of%20an%20operator., https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/execution/parallel/
* default time characteristic is event time - https://nightlies.apache.org/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/class-use/TimeCharacteristic.html  
* Deserialization - https://stackoverflow.com/questions/57266072/how-to-get-the-processing-kafka-topic-name-dynamically-in-flink-kafka-consumer
* Serialization - https://stackoverflow.com/questions/58644549/how-to-implement-flinkkafkaproducer-serializer-for-kafka-2-2
* Dese/se - https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/datastream/kafka/
https://nightlies.apache.org/flink/flink-docs-release-1.12/dev/connectors/kafka.html
* deduplication - https://stackoverflow.com/questions/35599069/apache-flink-0-10-how-to-get-the-first-occurence-of-a-composite-key-from-an-unbo
* CLI - https://nightlies.apache.org/flink/flink-docs-master/docs/deployment/cli/
* docker - https://nightlies.apache.org/flink/flink-docs-master/docs/deployment/resource-providers/standalone/docker/
* docker image - https://hub.docker.com/_/flink
* in flink, only the latest checkpoint is retained. When the job is killed or cancelled, the checkpoint is discarded also.
  User can manually create a save point to restart from the last offset
* known issue with kafka2.3 - https://stackoverflow.com/questions/51036351/kafka-unknown-producer-id-exception
  
  ```
    When a streams application has little traffic, then it is possible that consumer purging would delete even the last 
    message sent by a producer (i.e., all the messages sent by this producer have been consumed and committed), and as a 
    result, the broker would delete that producer's ID. The next time when this producer tries to send, it will get this 
    UNKNOWN_PRODUCER_ID error code, but in this case, this error is retriable: the producer would just get a new producer id
    and retries, and then this time it will succeed.

    org.apache.flink.util.FlinkRuntimeException: Failed to send data to Kafka txn-0@-1 with FlinkKafkaInternalProducer{transactionalId='kafka-sink-0-3', 
    inTransaction=true, closed=false} because of a bug in the Kafka broker (KAFKA-9310). Please upgrade to Kafka 2.5+. 
    If you are running with concurrent checkpoints, you also may want to try without them.
    To avoid data loss, the application will restart.
      at org.apache.flink.connector.kafka.sink.KafkaWriter$WriterCallback.throwException(KafkaWriter.java:405) ~[?:?]
      at org.apache.flink.connector.kafka.sink.KafkaWriter$WriterCallback.lambda$onCompletion$0(KafkaWriter.java:391) ~[?:?]
      at org.apache.flink.streaming.runtime.tasks.StreamTaskActionExecutor$1.runThrowing(StreamTaskActionExecutor.java:50) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.streaming.runtime.tasks.mailbox.Mail.run(Mail.java:90) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.streaming.runtime.tasks.mailbox.MailboxProcessor.processMailsWhenDefaultActionUnavailable(MailboxProcessor.java:338) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.streaming.runtime.tasks.mailbox.MailboxProcessor.processMail(MailboxProcessor.java:324) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.streaming.runtime.tasks.mailbox.MailboxProcessor.runMailboxLoop(MailboxProcessor.java:201) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.streaming.runtime.tasks.StreamTask.runMailboxLoop(StreamTask.java:809) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.streaming.runtime.tasks.StreamTask.invoke(StreamTask.java:761) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.runtime.taskmanager.Task.runWithSystemExitMonitoring(Task.java:958) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.runtime.taskmanager.Task.restoreAndInvoke(Task.java:937) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.runtime.taskmanager.Task.doRun(Task.java:766) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at org.apache.flink.runtime.taskmanager.Task.run(Task.java:575) ~[flink-dist_2.11-1.14.4.jar:1.14.4]
      at java.lang.Thread.run(Thread.java:750) ~[?:1.8.0_322]
      Caused by: org.apache.kafka.common.errors.UnknownProducerIdException: This exception is raised by the broker if it could not locate the producer metadata associated with the producerId in question. This could happen if, for instance, the producer's records were deleted because their retention time had elapsed. Once the last records of the producerId are removed, the producer's metadata is removed from the broker, and future appends by the producer will return this exception.
  ```
   
### Druid

* Kafka ingestion - https://druid.apache.org/docs/latest/development/extensions-core/kafka-ingestion.html
* Native query - https://druid.apache.org/docs/latest/querying/querying.html