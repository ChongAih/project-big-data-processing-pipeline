import json
import threading

from app.mykafka.mykafkaconsumer import MyKafkaConsumer
from app.util.mylogger import MyLogger
from config import Config

def test_consumption():
    consumer = MyKafkaConsumer(topic=Config.KAFKA_SRC_ORDER_TOPIC, group_id=None,
                               brokers=[Config.KAFKA_BOOTSTRAP_SERVER], auto_offset_reset="latest")
    for message in consumer.client:
        try:
            message_json = json.loads(message.value)
            consumer.logger.info((f"{message.topic}_{message.partition}: "
                                  f"{message_json}"))
        except Exception as e:
            consumer.logger.error("Error in reading message", exc_info=e)

def test_seek_to_beginning_and_poll():
    consumer = MyKafkaConsumer(topic=Config.KAFKA_SRC_ORDER_TOPIC, group_id=None,
                               brokers=[Config.KAFKA_BOOTSTRAP_SERVER], auto_offset_reset="latest")
    print(consumer.seek_to_beginning_and_poll(Config.KAFKA_SRC_ORDER_TOPIC))

def test_seek_and_poll_from_timestamps(timestamp_ms: int):
    consumer = MyKafkaConsumer(topic=Config.KAFKA_SRC_ORDER_TOPIC, group_id=None,
                               brokers=[Config.KAFKA_BOOTSTRAP_SERVER], auto_offset_reset="latest")
    print(consumer.seek_and_poll_from_timestamps(timestamp_ms, Config.KAFKA_SRC_ORDER_TOPIC))


if __name__ == "__main__":
    logger = MyLogger("kafka_main")

    try:
        logger.info("Testing consumption...")
        test_consumption()
    except KeyboardInterrupt:
        logger.info("Exit from test_consumption()")
    else:
        logger.error(f"Error occurs")

    try:
        logger.info("Testing seek_to_beginning_and_poll...")
        test_seek_to_beginning_and_poll()
    except KeyboardInterrupt:
        logger.error(f"test_seek_to_beginning_and_poll")

    try:
        logger.info("Testing seek_and_poll_from_timestamps...")
        test_seek_and_poll_from_timestamps(1645159680000)
    except KeyboardInterrupt:
        logger.error(f"KeyboardInterrupt")