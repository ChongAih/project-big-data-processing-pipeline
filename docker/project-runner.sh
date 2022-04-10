#!/bin/sh

# Docker configuration
export KAFKA_INTERNAL_SERVER="broker:9092"
export KAFKA_BOOTSTRAP_SERVER="localhost:29092"
export KAFKA_SRC_ORDER_TOPIC="order"
export KAFKA_DST_TXN_TOPIC="txn"
export KAFKA_DST_TXN_USER_TOPIC="txn_user"
export SPARK_MASTER="spark://spark-master:7077" # unify spark_master url for internal communication
export SPARK_SUBMIT_HOSTNAME="spark-client"

command="$1"

# Processing job configuration
if [ -n "$2" ]; then
  export JOB="$2"
else
  export JOB="Txn"
fi
if [ -n "$3" ]; then
  export CONFIG_RESOURCE_PATH="$3"
else
  export CONFIG_RESOURCE_PATH="txn.conf"
fi
if [ -n "$4" ]; then
  export KAFKA_START_TIME="$4"
else
  export KAFKA_START_TIME="-1"
fi
if [ -n "$5" ]; then
  export KAFKA_END_TIME="$5"
else
  export KAFKA_END_TIME="-1"
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

if [ $command = "start" ]; then
  echo && echo "=========== MAVEN JAVA & SCALA JAR PACKAGING ===========" && echo
  cd $SCRIPT_DIR/.. && mvn clean package
  cd $SCRIPT_DIR
  echo && echo "================== DOCKER COMPOSING UP ==================" && echo
  echo "Job setting:"
  echo "  * JOB - $JOB"
  echo "  * CONFIG_RESOURCE_PATH - $CONFIG_RESOURCE_PATH"
  echo "  * KAFKA_START_TIME - $KAFKA_START_TIME"
  echo "  * KAFKA_END_TIME - $KAFKA_END_TIME"
  echo
  docker-compose -f docker-compose.yml up -d
  echo && echo "=================== DOCKER EXECUTING ===================" && echo
  sleep 10
  # Create HBase table and write byte array data
  docker container exec project_hbase bash -c "echo \"create 'deduplication','cf'\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"create 'exchange_rate','cf'\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','ID$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.00007)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','MY$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.24)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','SG$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.74)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','PH$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.02)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"scan 'exchange_rate'\" | hbase shell -n"
  # Run spark processing job in cluster mode, no ACL is setup for Kafka cluster by default
  docker container exec project_spark_submit bash -c \
    "spark-submit \
    --verbose \
    --master ${SPARK_MASTER} \
    --deploy-mode cluster \
    --driver-memory 4G \
    --num-executors 3 \
    --executor-cores 1 \
    --executor-memory 1G \
    --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5 \
    --files \"/project-data-processing-pipeline/src/main/resources/log4j.properties,/project-data-processing-pipeline/src/main/resources/kafka_jaas.conf\" \
    --conf spark.jars.ivy=/opt/bitnami/spark/ivy \
    --conf spark.speculation=false \
    --conf \"spark.driver.extraJavaOptions=-Dlog4j.configuration=log4j.properties -Djava.security.auth.login.config=kafka_jaas.conf\" \
    --conf \"spark.executor.extraJavaOptions=-Dlog4j.configuration=log4j.properties -Djava.security.auth.login.config=kafka_jaas.conf\" \
    --conf spark.driver.extraClassPath=/opt/bitnami/spark/ivy/jars/* \
    --conf spark.executor.extraClassPath=/opt/bitnami/spark/ivy/jars/* \
    --class streaming.spark.StreamingRunner \
    /project-data-processing-pipeline/target/project-big-data-processing-pipeline-1.0-SNAPSHOT.jar \
    --job ${JOB} --config-resource-path ${CONFIG_RESOURCE_PATH} --kafka-start-time ${KAFKA_END_TIME} --kafka-end-time ${KAFKA_END_TIME}"
elif [ $command = "stop" ]; then
  echo && echo "================== DOCKER COMPOSING DOWN =================" && echo
  # Spark is using external network set in Kafka, so it should be executed prior to Kafka during down
  docker-compose -f docker-compose.yml down -v
  echo && echo "========================= CLEANING =======================" && echo
  # Clear volume bint mount directory
  KAFKA_DATA_DIR="$PWD/kafka"
  if [ -d $KAFKA_DATA_DIR ]; then rm -Rf $KAFKA_DATA_DIR; fi
  SPARK_PACKAGE_JAR_DIR="$PWD/jars"
  if [ -d SPARK_PACKAGE_JAR_DIR ]; then rm -Rf $SPARK_PACKAGE_JAR_DIR; fi
else
  echo "sh project-runner.sh <start | stop> [optional job_name] [optional resource_path] [optional kafka_start_time] [optional kafka_end_time] | [optional acl]"
  echo "<start | stop> start or stop all docker container"
  echo "<job_name> optional processing job class name. e.g, Txn/TxnUser. Default is set to be 'Txn'"
  echo "<resource_path> optional job configuration file name. e.g, txn.conf/txn_user.conf. Default is set to be txn.conf"
  echo "<kafka_start_time> optional Kafka consumption start time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1"
  echo "<kafka_end_time> optional Kafka consumption end time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1"
  exit 1
fi