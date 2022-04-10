/* project-big-data-processing-pipeline
 *
 * Created: 19/3/22 9:13 am
 *
 * Description:
 */
package streaming.flink.processor

import org.apache.flink.api.common.serialization.SerializationSchema
import streaming.KafkaOutputData

import java.nio.charset.StandardCharsets

class KafkaProducerSerialization extends SerializationSchema[KafkaOutputData]{
  override def serialize(data: KafkaOutputData): Array[Byte] = {
    data.kafkaValue.getBytes(StandardCharsets.UTF_8)
  }
}