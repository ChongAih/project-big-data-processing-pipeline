from typing import List, Dict, Set
from kafka import KafkaConsumer as Kc, TopicPartition
from kafka.consumer.fetcher import ConsumerRecord
from kafka.consumer.group import KafkaConsumer
from kafka.structs import OffsetAndTimestamp, OffsetAndMetadata
from app.util.mylogger import MyLogger


class MyKafkaConsumer():
    def __init__(self, brokers: List[str], topic: str, group_id = None, security_protocol: str = None, 
        sasl_mechanism: str = None, sasl_plain_username: str = None, 
        sasl_plain_password: str = None, auto_offset_reset: str = "latest", 
        consumer_timeout_ms: int = float("inf"), **kwargs):

        self.__dict__.update(kwargs)
        self.topic = topic
        self.brokers = brokers
        self.security_protocol = security_protocol
        self.sasl_mechanism = sasl_mechanism
        self.sasl_plain_username = sasl_plain_username
        self.sasl_plain_password = sasl_plain_password
        self.auto_offset_reset = auto_offset_reset
        self.group_id = group_id
        self.consumer_timeout_ms = consumer_timeout_ms

        self.logger = MyLogger("MyKafkaConsumer")
        self.logger.info(f'Connecting to Kafka servers: {brokers} - {topic}')
        # Set default KafkaConsumer with default None group-id
        self.client: KafkaConsumer = self.get_kafka_consumer(topic, brokers, security_protocol, 
             sasl_mechanism, sasl_plain_username, sasl_plain_password, 
             auto_offset_reset, group_id, consumer_timeout_ms)


    def get_kafka_consumer(self, topic: str, brokers, security_protocol: str = None, 
        sasl_mechanism: str = None, sasl_plain_username: str = None, 
        sasl_plain_password: str = None, auto_offset_reset: str = "latest", 
        group_id: str = None, consumer_timeout_ms: float = float("inf"), **kwargs) -> KafkaConsumer:
        options = dict()
        # Required manual assignment
        if not topic:
            if not security_protocol:
                consumer = Kc(
                    bootstrap_servers=brokers,
                    enable_auto_commit=False,
                    group_id=group_id,
                    auto_offset_reset=auto_offset_reset,
                    consumer_timeout_ms = consumer_timeout_ms,
                    **kwargs
                )
            else:
                consumer = Kc(
                    bootstrap_servers=brokers,
                    enable_auto_commit=False,
                    group_id=group_id,
                    auto_offset_reset=auto_offset_reset,
                    security_protocol=security_protocol,
                    sasl_mechanism=sasl_mechanism,
                    sasl_plain_username=sasl_plain_username,
                    sasl_plain_password=sasl_plain_password,
                    consumer_timeout_ms = consumer_timeout_ms,
                    **kwargs
                )
        # Assign/ subscribe to topics
        else:
            if not security_protocol:
                consumer = Kc(
                    topic,
                    bootstrap_servers=brokers,
                    enable_auto_commit=False,
                    group_id=group_id,
                    auto_offset_reset=auto_offset_reset,
                    consumer_timeout_ms = consumer_timeout_ms,
                    **kwargs
                )
            else:
                consumer = Kc(
                    topic,
                    bootstrap_servers=brokers,
                    enable_auto_commit=False,
                    group_id=group_id,
                    auto_offset_reset=auto_offset_reset,
                    security_protocol=security_protocol,
                    sasl_mechanism=sasl_mechanism,
                    sasl_plain_username=sasl_plain_username,
                    sasl_plain_password=sasl_plain_password,
                    consumer_timeout_ms = consumer_timeout_ms,
                    **kwargs
                )
        return consumer


    def seek_to_beginning_and_poll(self, topic: str = None) -> List[str]:
        """
        Use case: 
            import json

            messages = consumer.seek_to_beginning_and_poll()

            for msg in messages:
                try:
                    print(json.loads(msg))
                except Exception as e:
                    print(e)
        """
        return self.seek_and_poll(offsets=0, topic=topic)


    def seek_and_poll_from_timestamps(self, timestamp_ms: int, topic: str = None):
        """
        param: offset can be both List of Int 
               If it is a list, the length should be same as topic partitions 

        Use case:
            import json

            # if there is only a single partition
            messages = consumer.seek_and_poll_from_timestamps(1641033005)
            
            for msg in messages:
                try:
                    print(json.loads(msg))
                except Exception as e:
                    print(e)
        """
        def convert_to_ms(time_value: int) -> int:
            if ((0xFFFFFFFF00000000 & time_value) != 0):
                return time_value
            else:
                return time_value * 1000

        topic = self.topic if not topic else topic
        timestamp_ms = convert_to_ms(timestamp_ms)

        # Find offsets based on timestamps
        topic_partitions: List[TopicPartition] = self.get_topic_partition(topic)
        timestamps = dict()
        for tp in topic_partitions:
            timestamps[tp] = timestamp_ms
        offsets_dict: Dict[TopicPartition, OffsetAndTimestamp] = self.client.offsets_for_times(timestamps)
        
        # Get data based on given offsets
        offsets = []
        for _, oat in offsets_dict.items():
            # if oat = None that means no data at time >= timestamps
            offset = oat.offset if oat else None
            offsets.append(offset)

        return self.seek_and_poll(offsets, topic)


    def seek_and_poll(self, offsets, topic: str = None) -> List[str]:
        """
        param: offset can be both List of Int 
               If it is a list, the length should be same as topic partitions 

        Use case:
            import json

            # messages = consumer.seek_and_poll(0) - if there is only a single partition
            messages = consumer.seek_and_poll([0]) 
            
            for msg in messages:
                try:
                    print(json.loads(msg))
                except Exception as e:
                    print(e)
        """
        topic = self.topic if not topic else topic
        values: List[str] = []
        client = self.get_kafka_consumer(None, self.brokers, self.security_protocol, 
             self.sasl_mechanism, self.sasl_plain_username, self.sasl_plain_password, 
             self.auto_offset_reset, self.group_id, self.consumer_timeout_ms)

        # Set offset for each partition of the topic
        topic_partitions: List[TopicPartition] = self.get_topic_partition(topic)
        for i, tp in enumerate(topic_partitions):
            client.assign([tp])
            if offsets == None:
                return values
            elif isinstance(offsets, int):
                offset = offsets
            elif isinstance(offsets, list):
                offset = offsets[i]
            if offset == None: # if offset = None that means no data at time >= timestamps
                return values
            elif offset == 0:
                client.seek_to_beginning(tp)
            else:
                client.seek(tp, offset)
        
        partitions: Dict[TopicPartition, ConsumerRecord] = client.poll(timeout_ms=5000)
        if partitions:
            for p, messages in partitions.items():
                for msg in messages:
                    values.append(msg.value)

        return values

    
    def get_offset_lag(self, group_id: str, topic: str = None) -> int:
        # KafkaConsumer to have group-id to check last commited offset - committed(tp) 
        """
        consumer = MyKafkaConsumer(topic=Config.KAFKA_SRC_ORDER_TOPIC, group_id = "test",
            brokers=[Config.KAFKA_BOOTSTRAP_SERVER], auto_offset_reset="latest")
        for message in consumer.client:
            try: 
                message_json = json.loads(message.value)
                offset_lag = consumer.get_offset_lag("test", message.topic)
                consumer.logger.info((f"{message.topic}_{message.partition}: "
                    f"{message_json}"))
                consumer.logger.info((f"{message.topic} - offset_lag: {offset_lag}"))
            except Exception as e:
                consumer.logger.error("Error in reading message", exc_info=e)
        """
        topic = self.topic if not topic else topic
        topic_partitions: List[TopicPartition] = self.get_topic_partition(topic)
        self.logger.info(topic_partitions)
        offset_lag = 0
        client = self.get_kafka_consumer(None, self.brokers, self.security_protocol, 
            self.sasl_mechanism, self.sasl_plain_username, self.sasl_plain_password, 
            self.auto_offset_reset, self.group_id, self.consumer_timeout_ms)
        for tp in topic_partitions:
            client.assign([tp])
            last_commited_offset = self.client.committed(tp) or 0
            client.seek_to_end(tp)
            last_offset = client.position(tp)
            offset_lag += last_offset - last_commited_offset
            self.logger.info(f"last_offset: {last_offset}, last_commited_offset: {last_commited_offset}")

        return offset_lag


    def get_last_committed_offset(self, topic: str = None) -> Dict[TopicPartition, int]:
        topic = self.topic if not topic else topic
        topic_partitions: List[TopicPartition] = self.get_topic_partition(topic)
        last_committed_offsets = dict()
        for tp in topic_partitions:
            last_committed_offset = self.client.committed(tp)
            last_committed_offsets[tp] = last_committed_offset
        return last_committed_offsets
    
    def get_last_offset(self, topic: str = None) -> Dict[TopicPartition, int]:
        topic = self.topic if not topic else topic
        # Find offset lag given the group_id
        topic_partitions: List[TopicPartition] = self.get_topic_partition(topic)
        last_offsets = dict()
        for tp in topic_partitions:
            self.client.assign([tp])
            self.client.seek_to_end(tp)
            last_offset = self.client.position(tp)
            last_offsets[tp] = last_offset
        return last_offsets

    # this commits offsets only to Kafka
    def commit(self, topic: str = None, offset: int=1):
        topic = self.topic if not topic else topic
        topic_partitions: List[TopicPartition] = self.get_topic_partition(topic)
        offsets = dict()
        for tp in topic_partitions:
            offsets[tp] = OffsetAndMetadata(offset, None)
        self.client.commit(offsets)


    def get_topic_partition(self, topic: str) -> List[TopicPartition]:
        tps: Set[int] = self.client.partitions_for_topic(topic)
        topic_partitions = [TopicPartition(topic, partition) for partition in tps]
        return topic_partitions


    def __enter__(self):
        """
        Use case:
            with MyKafkaConsumer(topics=Config.KAFKA_SRC_ORDER_TOPIC, 
                brokers=[Config.KAFKA_BOOTSTRAP_SERVER]) as consumer:
                for message in consumer:
                    print ("%s:%d:%d: key=%s value=%s, typevalue=%s" 
                        % (message.topic, message.partition,
                            message.offset, message.key,
                            message.value, type(message.value)))
        """
        return self.client


    def __exit__(self, exc_type, exc_val, exc_tb):
        # do no commit the offset that has been consumed in this class
        self.client.close(autocommit=False) 
        self.client = None
        self.logger.warning('Close connection Kafka')
