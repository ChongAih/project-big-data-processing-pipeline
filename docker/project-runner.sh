#!/bin/sh

# Docker configuration
export KAFKA_INTERNAL_SERVER="broker:9092"
export KAFKA_BOOTSTRAP_SERVER="localhost:29092"
export KAFKA_SRC_ORDER_TOPIC="order"
export KAFKA_DST_TXN_TOPIC="txn"
export KAFKA_DST_TXN_USER_TOPIC="txn_user"
export SPARK_MASTER="spark://spark-master:7077" # unify spark_master url for internal communication
export SPARK_SUBMIT_HOSTNAME="spark-client"

# Processing job configuration
export JOB="Txn"
export CONFIG_RESOURCE_PATH="txn.conf"
export KAFKA_START_TIME="-1"
export KAFKA_END_TIME="-1"

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

command="$1"

# sh project-runner.sh start
if [ $command = "start" ]; then
  cd $SCRIPT_DIR/.. && mvn clean package
  cd $SCRIPT_DIR
  docker-compose -f docker-compose.yml up -d
# sh project-runner.sh stop
elif [ $command = "stop" ]; then
  # Spark is using external network set in Kafka, so it should be executed prior to Kafka during down
  docker-compose -f docker-compose.yml down -v
  # Clear volume bint mount directory
  KAFKA_DATA_DIR="$PWD/kafka"
  if [ -d $KAFKA_DATA_DIR ]; then rm -Rf $KAFKA_DATA_DIR; fi
else
  echo "The input command is invalid, only 'start' or 'stop' is allowed"
fi

