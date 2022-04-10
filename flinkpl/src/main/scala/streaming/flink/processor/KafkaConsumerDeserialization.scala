/* project-big-data-processing-pipeline
 *
 * Created: 19/3/22 9:02 am
 *
 * Description:
 */
package streaming.flink.processor

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.kafka.clients.consumer.ConsumerRecord
import streaming.KafkaInputData

import java.nio.charset.StandardCharsets

class KafkaConsumerDeserialization extends KafkaDeserializationSchema[KafkaInputData]{
  override def isEndOfStream(t: KafkaInputData): Boolean = false

  override def deserialize(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]): KafkaInputData = {
    val kafkaValue = new String(consumerRecord.value(), StandardCharsets.UTF_8)
    val kafkaTime = (consumerRecord.timestamp() / 1000).toInt
    KafkaInputData(kafkaValue, kafkaTime)
  }

  override def getProducedType: TypeInformation[KafkaInputData] = {
    TypeInformation.of(classOf[KafkaInputData])
  }
}