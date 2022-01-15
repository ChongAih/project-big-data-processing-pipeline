import json
import random
import threading
import time
from datetime import datetime as dt

from app.mykafka.mykafkaconsumer import MyKafkaConsumer
from app.mykafka.mykafkaproducer import MyKafkaProducer
from app.util.mylogger import MyLogger
from config import Config

FIELDS = ["currency", "order_status", "userid", "gmv", "create_time"]
CURRENCIES = {
    "values": ["IDR", "PHP", "MYR", "SGD"],
    "weights": [15, 6, 4, 2]
}
STATUSES = {
    "values": ["paid", "processing", "failed"],
    "weights": [20, 5, 1]
}
PRICE_RANGE = {
    "IDR": {
        "min": 10000,
        "max": 1000000
    },
    "PHP": {
        "min": 40,
        "max": 4000
    },
    "MYR": {
        "min": 3,
        "max": 300
    },
    "SGD": {
        "min": 1,
        "max": 100
    }
}


def generation_main():
    producer = MyKafkaProducer(
        topic=Config.KAFKA_SRC_ORDER_TOPIC,
        brokers=[Config.KAFKA_BOOTSTRAP_SERVER]
    )

    while True:
        data = dict()
        data["create_time"] = int(dt.now().strftime("%s"))
        data["currency"] = random.choices(CURRENCIES["values"], weights=CURRENCIES["weights"], k=1)[0]
        data["order_status"] = random.choices(STATUSES["values"], weights=STATUSES["weights"], k=1)[0]
        data["user_id"] = random.randint(0, 1000000)
        data["order_id"] = random.randint(0, 10000000)
        data["gmv"] = random.uniform(PRICE_RANGE[data["currency"]]["min"],
                                     PRICE_RANGE[data["currency"]]["max"])
        data["event"] = {"database": Config.KAFKA_SRC_ORDER_TOPIC}
        producer.send(value=data)
        time.sleep(Config.KAFKA_GENERATION_INTERVAL)


def consumption_main():
    consumer = MyKafkaConsumer(topic=Config.KAFKA_SRC_ORDER_TOPIC, group_id=None,
                               brokers=[Config.KAFKA_BOOTSTRAP_SERVER], auto_offset_reset="latest")
    for message in consumer.client:
        try:
            message_json = json.loads(message.value)
            consumer.logger.info((f"{message.topic}_{message.partition}: "
                                  f"{message_json}"))
        except Exception as e:
            consumer.logger.error("Error in reading message", exc_info=e)


if __name__ == "__main__":
    logger = MyLogger("kafka_main")
    try:
        logger.info(f"The current threading number: {threading.active_count()}")
        thread_producer = threading.Thread(target=generation_main)
        thread_consumer = threading.Thread(target=consumption_main)
        thread_producer.start()
        thread_consumer.start()
        logger.info(f"The current threading number: {threading.active_count()}")
        thread_producer.join()  # Wait till thread has completed
        thread_consumer.join()
    except KeyboardInterrupt:
        logger.error(f"KeyboardInterrupt")
