import json
from typing import List
from kafka import KafkaProducer as Kp
from app.util.mylogger import MyLogger


class MyKafkaProducer():
    def __init__(self, topic: str, brokers: List[str], 
        security_protocol: str = None, sasl_mechanism: str = None, 
        sasl_plain_username: str = None, sasl_plain_password: str = None, 
        value_serializer: callable = lambda v: json.dumps(v).encode('utf-8'),
        **kwargs):

        self.__dict__.update(kwargs)
        self.topic = topic

        self.logger = MyLogger("MyKafkaProducer")
        self.logger.info(f'Connecting to Kafka servers: {brokers} - {topic}')

        # Assign/ subscribe to topics
        if not security_protocol:
            self.client = Kp(
                bootstrap_servers=brokers,
                value_serializer=value_serializer,
                **kwargs
            )
        else:
            self.client = Kp(
                bootstrap_servers=brokers,
                value_serializer=value_serializer,
                security_protocol=security_protocol,
                sasl_mechanism=sasl_mechanism,
                sasl_plain_username=sasl_plain_username,
                sasl_plain_password=sasl_plain_password,
                **kwargs
            )

            
    def send(self, topic: str = None, key: str = None, value: str = None, 
        timestamp_ms: int = None):

        def convert_to_ms(time_value: int) -> int:
            if ((0xFFFFFFFF00000000 & time_value) != 0):
                return time_value
            else:
                return time_value * 1000

        topic = self.topic if not topic else topic
        
        if self.client.bootstrap_connected:
            if timestamp_ms:
                self.client.send(topic=topic, key=key, value=value, 
                    timestamp_ms=convert_to_ms(timestamp_ms))
            else:
                self.client.send(topic=topic, key=key, value=value)

