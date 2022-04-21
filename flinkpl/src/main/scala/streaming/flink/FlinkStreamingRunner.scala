/* project-big-data-processing-pipeline
 *
 * Created: 17/3/22 6:42 pm
 *
 * Description:
 */
package streaming.flink

import com.typesafe.config.Config
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.base.DeliveryGuarantee
import org.apache.flink.connector.kafka.sink.{KafkaRecordSerializationSchema, KafkaSink}
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema
import org.apache.flink.streaming.api.scala.{createTypeInformation, DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.flink.table.api.{EnvironmentSettings, Table}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.log4j.Logger
import util.ArgumentParser
import streaming.{KafkaInputData, KafkaOutputData}
import streaming.flink.job.FlinkJob
import streaming.flink.processor.{KafkaConsumerDeserialization, KafkaProducerSerialization}
import util.{ConfigReader, Const, LoggerCreator}

import java.util.Properties
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object FlinkStreamingRunner extends FlinkStreamingRunnerHelper {
  def main(args: Array[String]): Unit = {
    run(Array("--job", "FlinkTxn",
      "--config-resource-path", "flink_txn.conf",
      "--kafka-start-time", "1642157787238",
      "--kafka-end-time", "-1",
      "--local"))
  }
}


class FlinkStreamingRunnerHelper extends FlinkSettingHelper {

  val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)

  def run(args: Array[String]): Unit = {

    // Read command line arguments
    val argumentParser = new ArgumentParser(args)
    val jobName = {
      argumentParser.job
        .getOrElse(throw new IllegalArgumentException("Missing --job"))
    }
    val configResourcePath = {
      argumentParser.configResourcePath
        .getOrElse("")
    }
    val kafkaStartTime = {
      argumentParser.kafkaStartTime
        .getOrElse(Const.KAFKA_DEFAULT_LATEST_OFFSET)
        .toLong
    }
    val kafkaEndTime = {
      argumentParser.kafkaEndTime
        .getOrElse(Const.KAFKA_DEFAULT_LATEST_OFFSET)
        .toLong
    }
    val local = argumentParser.local.getOrElse(Const.SPARK_LOCAL_MASTER)

    logger.info(
      s"""
         |User set parameters:
         |  * jobName: $jobName
         |  * configResourcePath: $configResourcePath
         |  * kafkaStartTime: $kafkaStartTime
         |  * kafkaEndTime: $kafkaEndTime
         |  * local: $local
         |""".stripMargin)

    // Read configuration & setup environment
    val config: Config = ConfigReader.readConfig(s"config/$configResourcePath")
    val flinkConfig: Config = config.getConfig("flink")
    val senv = getFlinkStreamExecutionEnv(flinkConfig)
    val tenv = getFlinkStreamTableEnv(senv, flinkConfig)

    // Setup processing job & register UDF
    val job = {
      Class.forName(s"streaming.flink.job.$jobName")
        .getConstructor()
        .newInstance()
        .asInstanceOf[FlinkJob]
    }
    job.registerUDF(tenv, config)

    // Read Kafka stream and create temporary view from the respective processed stream output
    // Set uid to record state/ offset of Kafka topic
    val kafkaStream: DataStream[KafkaInputData] = senv.fromSource(
      getKafkaSource(
        config.getConfig("kafka"),
        kafkaStartTime,
        new KafkaConsumerDeserialization
      ),
      WatermarkStrategy.noWatermarks(),
      "input"
    )
      .name(config.getConfig("kafka").getString("input.topic") + "_" + jobName)
      .uid(config.getConfig("kafka").getString("input.topic") + "_" + jobName)

    job.createInputTempView(tenv, kafkaStream)

    // Run business logic SQL
    val sqlTable: Table = tenv.sqlQuery(job.getSqlQuery)
    val sqlDataStream: DataStream[KafkaOutputData] = job.getOutputDataStream(tenv, sqlTable)
    val outputDataStream: DataStream[KafkaOutputData] = job.deduplicationAndTimeFilter(sqlDataStream)

    // Write to console or sink
    // Set uid to record state/ offset of Kafka topic
    if (local) {
      outputDataStream.print()
    } else {
      outputDataStream.sinkTo(
        getKafkaSink[KafkaOutputData](
          config.getConfig("kafka"),
          new KafkaProducerSerialization
        )
      )
        .name(config.getConfig("kafka").getString("output.topic") + "_" + jobName)
        .uid(config.getConfig("kafka").getString("output.topic") + "_" + jobName)
    }

    senv.execute(jobName)
  }


  def getKafkaSink[T](kafkaConfig: Config,
                   schema: SerializationSchema[T],
                   deliveryGuarantee: DeliveryGuarantee = DeliveryGuarantee.EXACTLY_ONCE): KafkaSink[T] = {
    val recordSerializer: KafkaRecordSerializationSchema[T] = {
      KafkaRecordSerializationSchema.builder[T]()
        .setTopic(kafkaConfig.getString("output.topic"))
        .setValueSerializationSchema(schema)
        .build()
    }

    val sink = KafkaSink.builder[T]()
      .setBootstrapServers(kafkaConfig.getString("output.bootstrap_servers"))
      .setDeliverGuarantee(deliveryGuarantee)
      .setRecordSerializer(recordSerializer)

    // TODO: ACL
//    if (kafkaConfig.hasPath("output.acl") && kafkaConfig.getBoolean("output.acl")) {
//      val properties = new Properties()
//      val kafkaCommonConfig = kafkaConfig.getConfig("common").entrySet().asScala
//      kafkaCommonConfig.foreach(item => {
//        properties.setProperty(item.getKey, item.getValue.toString)
//      })
//      sink.build()
//    }

    sink.build()
  }

  def getKafkaSource[T](kafkaConfig: Config,
                        kafkaStartTime: Long,
                        schema: KafkaDeserializationSchema[T],
                        isPatternTopic: Boolean = false): KafkaSource[T] = {
    // Set common configuration
    val source = KafkaSource.builder[T]()
      .setBootstrapServers(kafkaConfig.getString("input.bootstrap_servers"))
      .setGroupId(kafkaConfig.getString("input.group_id"))
      .setDeserializer(KafkaRecordDeserializationSchema.of(schema))

    if (kafkaConfig.hasPath("input.acl") && kafkaConfig.getBoolean("input.acl")) {
      val properties = new Properties()
      val kafkaCommonConfig = kafkaConfig.getConfig("common").entrySet().asScala
      kafkaCommonConfig.foreach(item => {
        properties.setProperty(item.getKey, item.getValue.toString)
      })
      source.setProperties(properties)
    }

    // Subscribe to topics
    val kafkaTopic = kafkaConfig.getString("input.topic")
    if (isPatternTopic) {
      source.setTopicPattern(java.util.regex.Pattern.compile(kafkaTopic))
    } else {
      val topicList = kafkaTopic.split(",").toList.asJava
      source.setTopics(topicList)
    }

    kafkaStartTime match {
      case x if x > 0 => {
        source.setStartingOffsets(OffsetsInitializer.timestamp(kafkaStartTime))
      }
      case x if x == -1 => source.setStartingOffsets(OffsetsInitializer.latest())
      case x if x == -2 => source.setStartingOffsets(OffsetsInitializer.earliest())
      case _ => source.setStartingOffsets(OffsetsInitializer.latest())
    }

    // Dynamically discover new partition of a Kafka topic
    source.setProperty("partition.discovery.interval.ms", "300000")

    source.build()
  }

}