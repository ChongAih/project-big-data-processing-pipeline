/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 6:41 pm
 *
 * Description:
 */
package util

import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetAndTimestamp}
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.log4j.Logger
import util.ConversionHelper.getMilliSecond

import java.{lang, util}
import java.text.SimpleDateFormat
import java.util.{Date, Properties}
import scala.collection.JavaConversions._

object KafkaHelper {
  val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)

  def getKafkaStartingOffsets(
                               kafkaSrcServers: String,
                               kafkaSrcTopics: String,
                               kafkaStartTime: Long,
                               kafkaAclEnabled: Boolean = false,
                               kafkaSecurityProtocol: String = "",
                               kafkaSaslMechanism: String = ""
                             ): String = {
    kafkaStartTime match {
      case x if x > 0 => getKafkaPartitionOffsets(kafkaSrcServers,
        kafkaSrcTopics, kafkaStartTime, kafkaAclEnabled,
        kafkaSecurityProtocol, kafkaSaslMechanism)
      case x if x == -1 => "latest"
      case x if x == -2 => "earliest"
      case _ => "latest"
    }
  }

  def getKafkaEndingOffsets(
                             kafkaSrcServers: String,
                             kafkaSrcTopics: String,
                             kafkaEndTime: Long,
                             kafkaAclEnabled: Boolean = false,
                             kafkaSecurityProtocol: String = "",
                             kafkaSaslMechanism: String = ""
                           ): String = {
    kafkaEndTime match {
      case x if x > 0 => getKafkaPartitionOffsets(kafkaSrcServers,
        kafkaSrcTopics, kafkaEndTime, kafkaAclEnabled,
        kafkaSecurityProtocol, kafkaSaslMechanism)
      case x if x == -1 => "latest"
      case _ => "latest"
    }
  }

  def getKafkaPartitionOffsets(
                                kafkaServers: String,
                                kafkaSrcTopics: String,
                                fetchDataTime: Long,
                                kafkaAclEnabled: Boolean,
                                kafkaSecurityProtocol: String,
                                kafkaSaslMechanism: String
                              ): String = {

    // KafkaHelper.getKafkaPartitionOffsets("localhost:29092", "order", 1641046500L, false, "", "")

    // Instantiate a KafkaConsumer
    val prop = new Properties()
    prop.put("bootstrap.servers", kafkaServers)
    prop.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    prop.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    if (kafkaAclEnabled) {
      prop.put("security.protocol", kafkaSecurityProtocol)
      prop.put("sasl.mechanism", kafkaSaslMechanism)
    }
    val consumer = new KafkaConsumer(prop)

    // Check offsets of each topic partition based on timestamp
    var topicOffsets: Map[String, Map[String, Long]] = Map()
    val df: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    try {
      kafkaSrcTopics.split(",").foreach(kafkaTopic => {
        // Construct timestamps map & search offsets
        val fetchDataTimeMs: Long = getMilliSecond(fetchDataTime)
        val timestampsToSearch: util.HashMap[TopicPartition, Long] = new util.HashMap()
        val partitionInfos: util.List[PartitionInfo] = consumer.partitionsFor(kafkaTopic)
        for (pInfo <- partitionInfos) {
          timestampsToSearch.put(new TopicPartition(pInfo.topic(), pInfo.partition()), fetchDataTimeMs)
        }
        val offsetsForTimes: util.Map[TopicPartition, OffsetAndTimestamp] = {
          consumer.offsetsForTimes(timestampsToSearch.asInstanceOf[util.Map[TopicPartition, lang.Long]])
        }
        logger.info(s"fetchDataTimeMs: $fetchDataTimeMs, offsetsForTimes : $offsetsForTimes")

        // Construct offsets string
        var singleTopicOffsets: Map[String, Long] = Map()
        for (entry <- offsetsForTimes) {
          val tp: TopicPartition = entry._1
          val oat: OffsetAndTimestamp = entry._2
          if (oat != null && fetchDataTimeMs != 0L) {
            val partition = tp.partition()
            val topic = tp.topic()
            val timestamp = oat.timestamp()
            val offset = oat.offset()
            singleTopicOffsets += (s"$partition" -> offset)
            logger.info("topic = " + topic +
              ", partition = " + partition +
              ", time = " + df.format(new Date(timestamp)) +
              ", offset = " + offset)
          } else {
            val partition = tp.partition()
            singleTopicOffsets += (s"$partition" -> -1)
          }
        }
        topicOffsets += (kafkaTopic -> singleTopicOffsets)
      })
    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      consumer.close()
    }

    // Return JSON string
    val mapper = new JacksonScalaObjectMapper
    val offsetsStr = mapper.serialize(topicOffsets)
    logger.info(s"offsets : $offsetsStr")
    offsetsStr
  }
}