/* project-big-data-processing-pipeline
 *
 * Created: 21/3/22 7:22 pm
 *
 * Description:
 */
package streaming.flink.processor

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector
import streaming.KafkaInputData
import util.JacksonScalaObjectMapper

abstract class FlinkKafkaConsumerMessageProcessor[T]
  extends ProcessFunction[KafkaInputData, T] {

  var objectMapper: JacksonScalaObjectMapper = _

  override def open(parameters: Configuration): Unit = {
    objectMapper = new JacksonScalaObjectMapper()
    super.open(parameters)
  }

  override def processElement(data: KafkaInputData,
                              context: ProcessFunction[KafkaInputData, T]#Context,
                              collector: Collector[T]): Unit = {
    processKafkaConsumerMessage(data, context, collector)
  }

  def processKafkaConsumerMessage(data: KafkaInputData,
                                  context: ProcessFunction[KafkaInputData, T]#Context,
                                  collector: Collector[T]): Unit
}