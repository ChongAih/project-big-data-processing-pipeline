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
  cd $SCRIPT_DIR/.. && mvn clean package
  cd $SCRIPT_DIR
  echo "JOB: $JOB; CONFIG_RESOURCE_PATH: $CONFIG_RESOURCE_PATH; KAFKA_START_TIME: $KAFKA_START_TIME; KAFKA_END_TIME: $KAFKA_END_TIME"
  docker-compose -f docker-compose.yml up -d
elif [ $command = "stop" ]; then
  # Spark is using external network set in Kafka, so it should be executed prior to Kafka during down
  docker-compose -f docker-compose.yml down -v
  # Clear volume bint mount directory
  KAFKA_DATA_DIR="$PWD/kafka"
  if [ -d $KAFKA_DATA_DIR ]; then rm -Rf $KAFKA_DATA_DIR; fi
  SPARK_PACKAGE_JAR_DIR="$PWD/jars"
  if [ -d SPARK_PACKAGE_JAR_DIR ]; then rm -Rf $SPARK_PACKAGE_JAR_DIR; fi
else
  echo "sh project-runner.sh <start | stop> [optional job_name] [optional resource_path] [optional kafka_start_time] [optional kafka_end_time]"
  echo "<start | stop> start or stop all docker container"
  echo "<job_name> optional processing job class name. e.g, Txn/TxnUser. Default is set to be 'Txn'"
  echo "<resource_path> optional job configuration file name. e.g, txn.conf/txn_user.conf. Default is set to be txn.conf"
  echo "<kafka_start_time> optional Kafka consumption start time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1"
  echo "<kafka_end_time> optional Kafka consumption end time (millisecond, second will get converted). e.g, 1643441056000. Default is set to be -1"
  exit 1
fi