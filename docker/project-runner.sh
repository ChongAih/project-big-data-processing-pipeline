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

if [ -n "$2" ]; then
  pipeline="$2"
else
  pipeline="spark"
fi

# Processing job configuration
if [ $pipeline = "flink" ]; then
  if [ -n "$3" ]; then
    export JOB="$3"
  else
    export JOB="FlinkTxn"
  fi
  if [ -n "$4" ]; then
    export CONFIG_RESOURCE_PATH="$4"
  else
    export CONFIG_RESOURCE_PATH="flink_txn.conf"
  fi
else
  if [ -n "$3" ]; then
    export JOB="$3"
  else
    export JOB="Txn"
  fi
  if [ -n "$4" ]; then
    export CONFIG_RESOURCE_PATH="$4"
  else
    export CONFIG_RESOURCE_PATH="txn.conf"
  fi
fi

if [ -n "$5" ]; then
  export KAFKA_START_TIME="$5"
else
  export KAFKA_START_TIME="-1"
fi
if [ -n "$6" ]; then
  export KAFKA_END_TIME="$6"
else
  export KAFKA_END_TIME="-1"
fi

if [ $JOB = "Txn" ] || [ $JOB = "FlinkTxn" ]; then
  DRUID_KAFKA_INDEX_JSON="$PWD/druid_txn_kafka_index.json"
  DRUID_QUERY_JSON="$PWD/druid_txn_query.json"
else
  DRUID_KAFKA_INDEX_JSON="$PWD/druid_txn_user_kafka_index.json"
  DRUID_QUERY_JSON="$PWD/druid_txn_user_query.json"
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

  echo && echo "=================== DOCKER EXECUTING (MIGHT TAKE SOME TIME) ===================" && echo

  sleep 30
  # Create HBase table and write byte array data
  docker container exec project_hbase bash -c "echo \"create 'deduplication','cf'\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"create 'exchange_rate','cf'\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','ID$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.00007)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','MY$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.24)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','SG$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.74)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"put 'exchange_rate','PH$(date '+%Y-%m-%d')','cf:exchange_rate',Bytes.toBytes(0.02)\" | hbase shell -n"
  docker container exec project_hbase bash -c "echo \"scan 'exchange_rate'\" | hbase shell -n"
  sleep 30
  if [ $pipeline = "flink" ]; then
    docker container exec project_flink_jobmanager bash -c \
      "./bin/flink run \
	    --detached \
      --class streaming.flink.FlinkStreamingRunner \
      /project-data-processing-pipeline/flinkpl/target/flinkpl-1.0-SNAPSHOT.jar \
      --job ${JOB} --config-resource-path ${CONFIG_RESOURCE_PATH} --kafka-start-time ${KAFKA_END_TIME} --kafka-end-time ${KAFKA_END_TIME}"
  else
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
      --files \"/project-data-processing-pipeline/sparkpl/src/main/resources/log4j.properties,/project-data-processing-pipeline/sparkpl/src/main/resources/kafka_jaas.conf\" \
      --conf spark.jars.ivy=/opt/bitnami/spark/ivy \
      --conf spark.speculation=false \
      --conf \"spark.driver.extraJavaOptions=-Dlog4j.configuration=log4j.properties -Djava.security.auth.login.config=kafka_jaas.conf\" \
      --conf \"spark.executor.extraJavaOptions=-Dlog4j.configuration=log4j.properties -Djava.security.auth.login.config=kafka_jaas.conf\" \
      --conf spark.driver.extraClassPath=/opt/bitnami/spark/ivy/jars/* \
      --conf spark.executor.extraClassPath=/opt/bitnami/spark/ivy/jars/* \
      --class streaming.spark.StreamingRunner \
      /project-data-processing-pipeline/sparkpl/target/sparkpl-1.0-SNAPSHOT.jar \
      --job ${JOB} --config-resource-path ${CONFIG_RESOURCE_PATH} --kafka-start-time ${KAFKA_END_TIME} --kafka-end-time ${KAFKA_END_TIME}"
  fi

  echo && echo "=================== SUBMITTING KAFKA INDEXING TASK TO DRUID OVERLOAD ===================" && echo

  curl -X POST \
    http://localhost:8086/druid/indexer/v1/supervisor \
    -H 'cache-control: no-cache' \
    -H 'content-type: application/json' \
    -d @$DRUID_KAFKA_INDEX_JSON

  echo && echo "=================== QUERYING DRUID DATASOURCE EVERY 1 MINUTE ===================" && echo

  while true
  do
    query=$(cat $DRUID_QUERY_JSON | sed "s/start_time/$(date +"%Y-%m-%d")T00:00:00+08:00/" | sed "s/end_time/$(date -v +1d +"%Y-%m-%d")T00:00:00+08:00/")
    curl -X POST \
      http://localhost:8889/druid/v2/?pretty \
      -H 'cache-control: no-cache' \
      -H 'content-type: application/json' \
      -d "$query"
    sleep 60
  done

elif [ $command = "stop" ]; then

  echo && echo "================== DOCKER COMPOSING DOWN =================" && echo

  docker-compose -f docker-compose.yml down -v
  docker-compose -f docker-compose.yml down -v

  echo && echo "========================= CLEANING =======================" && echo

  # Clear volume bin mount directory
  KAFKA_DATA_DIR="$PWD/kafka"
  if [ -d $KAFKA_DATA_DIR ]; then rm -Rf $KAFKA_DATA_DIR; fi
  SPARK_PACKAGE_JAR_DIR="$PWD/jars"
  if [ -d $SPARK_PACKAGE_JAR_DIR ]; then rm -Rf $SPARK_PACKAGE_JAR_DIR; fi
  DRUID_DATA_DIR="$PWD/druid"
  if [ -d $DRUID_DATA_DIR ]; then rm -Rf $DRUID_DATA_DIR; fi

else

  echo "sh project-runner.sh <start | stop> [optional processing_platform] [optional job_name] [optional resource_path] [optional kafka_start_time] [optional kafka_end_time] | [optional acl]"
  echo "<start | stop> start or stop all docker container"
  echo "<processing_platform> flink or spark. e.g, flink/spark. Default is set to be 'spark'"
  echo "<job_name> optional processing job class name. e.g, Txn/TxnUser. Default is set to be 'Txn'"
  echo "<resource_path> optional job configuration file name. e.g, txn.conf/txn_user.conf. Default is set to be txn.conf"
  echo "<kafka_start_time> optional Kafka consumption start time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1"
  echo "<kafka_end_time> optional Kafka consumption end time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1"
  exit 1

fi