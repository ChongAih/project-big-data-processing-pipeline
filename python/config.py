import os

class Config:
    KAFKA_BOOTSTRAP_SERVER = os.getenv("KAFKA_INTERNAL_SERVER") or "localhost:29092"
    KAFKA_SRC_ORDER_TOPIC = os.getenv("KAFKA_SRC_ORDER_TOPIC") or "order"
    KAFKA_DST_TXN_TOPIC = os.getenv("KAFKA_DST_TXN_TOPIC") or "txn"
    KAFKA_DST_TXN_USER_TOPIC = os.getenv("KAFKA_DST_TXN_USER_TOPIC") or "txn_user"
    KAFKA_GENERATION_INTERVAL = 0.2
