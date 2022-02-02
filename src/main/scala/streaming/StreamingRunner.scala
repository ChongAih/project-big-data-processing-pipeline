/* project-big-data-processing-pipeline
 *
 * Created: 29/12/21 2:37 pm
 *
 * Description:
 */
package streaming

import com.typesafe.config.Config
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.streaming.{DataStreamWriter, StreamingQuery, Trigger}
import streaming.job.Job
import util.{ConfigReader, Const, KafkaHelper, LoggerCreator}


object StreamingRunner extends StreamingRunnerHelper {

  def main(args: Array[String]): Unit = {
    run(args)
  }

}

trait StreamingRunnerHelper {

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
      argumentParser.kafkaStartTime
        .getOrElse(Const.KAFKA_DEFAULT_LATEST_OFFSET)
        .toLong
    }
    val local = argumentParser.sparkLocalMaster.getOrElse(Const.SPARK_LOCAL_MASTER)

    logger.info(
      s"""
         |User set parameters:
         |  * jobName: $jobName
         |  * configResourcePath: $configResourcePath
         |  * kafkaStartTime: $kafkaStartTime
         |  * kafkaEndTime: $kafkaEndTime
         |""".stripMargin)

    // Read configuration
    val config: Config = ConfigReader.readConfig(s"config/$configResourcePath")

    val sparkConf = {
      val conf = new SparkConf()
      conf.set("spark.app.name", jobName)
      if (local) {
        conf.set("spark.master", "local[6]")
        conf.set("spark.driver.bindAddress", "localhost")
      }
      conf
    }
    val spark = getSparkSession(sparkConf)

    val job = {
      Class.forName(s"streaming.job.$jobName")
        .getConstructor()
        .newInstance()
        .asInstanceOf[Job]
    }

    job.registerUDF(spark)

    val srcDF = getKafkaSrcDataFrame(spark, config, kafkaStartTime, kafkaEndTime)

    job.processRegisterInputTables(config, spark, srcDF)

    logger.info(
      s"""Spark SQL:
         |  ${job.getSQLText}
         |""".stripMargin)

    val dstDF = spark.sql(job.getSQLText)

    if (local) {
      // Since access docker hbase from local IDE requires update of host /etc/hosts file,
      // the deduplication filter is omitted to avoid tampering of files on host
      postDstDataFrameToConsole(dstDF, config)
    } else {
      postDstDataFrameToKafka(
        job.deduplicationAndTimeFilter(dstDF, config),
        config
      )
    }

  }


  def getSparkSession(sparkConf: SparkConf): SparkSession = {
    SparkSession
      .builder
      .config(sparkConf)
      .enableHiveSupport()
      .getOrCreate()
  }


  def getKafkaSrcDataFrame(sparkSession: SparkSession, config: Config,
                           kafkaStartTime: Long, kafkaEndTime: Long): DataFrame = {
    val kafkaSrcTopics = ConfigReader.getConfigField[String](config, "kafka.input.topic")
    val kafkaSrcServers = ConfigReader.getConfigField[String](config, "kafka.input.bootstrap_servers")
    val kafkaMaxTriggerOffset = ConfigReader.getConfigField[Long](config, "kafka.input.max_trigger_offsets")
    val kafkaGroupId = ConfigReader.getConfigField[String](config, "kafka.input.group.id") // Generated automatically if not given
    val aclSrc = ConfigReader.getConfigField[Boolean](config, "kafka.input.acl")
    val securityProtocol = ConfigReader.getConfigField[String](config, "kafka.common.security_protocol")
    val saslMechanism = ConfigReader.getConfigField[String](config, "kafka.common.sasl_mechanism")

    val startingOffsets = KafkaHelper.getKafkaStartingOffsets(kafkaSrcServers, kafkaSrcTopics,
      kafkaStartTime, aclSrc, securityProtocol, saslMechanism)
    val endingOffsets = KafkaHelper.getKafkaEndingOffsets(kafkaSrcServers, kafkaSrcTopics,
      kafkaEndTime, aclSrc, securityProtocol, saslMechanism)

    logger.info(s"kafkaSrcServers: $kafkaSrcServers; kafkaSrcTopics: $kafkaSrcTopics")
    logger.info(s"startingOffsets: $startingOffsets; endingOffsets: $endingOffsets")

    val df: DataFrame = kafkaEndTime match {
      // Batch query
      case e if e > kafkaStartTime && e > 0 => {
        var temp = sparkSession
          .read
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaSrcServers)
          //.option("kafka.group.id", kafkaGroupId)
          .option("subscribe", kafkaSrcTopics)
          .option("failOnDataLoss", "false")
          .option("startingOffsets", startingOffsets)
          .option("endingOffsets", endingOffsets)
          .option("maxOffsetsPerTrigger", kafkaMaxTriggerOffset)
        temp = if (aclSrc) {
          temp.option("kafka.sasl.mechanism", saslMechanism)
            .option("kafka.security.protocol", securityProtocol)
        } else {
          temp
        }
        temp.load()
      }
      // Stream query
      case _ => {
        var temp = sparkSession
          .readStream
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaSrcServers)
          .option("subscribe", kafkaSrcTopics)
          .option("failOnDataLoss", "false")
          .option("startingOffsets", startingOffsets)
          .option("maxOffsetsPerTrigger", kafkaMaxTriggerOffset)
        temp = if (aclSrc) {
          temp.option("kafka.sasl.mechanism", saslMechanism)
            .option("kafka.security.protocol", securityProtocol)
        } else {
          temp
        }
        temp.load()
      }
    }

    //df.printSchema()

    df.select(
      col(Const.KAFKA_VALUE_NAME).cast("string").as(Const.KAFKA_VALUE_NAME),
      col(Const.KAFKA_TIMESTAMP_NAME).cast("long").as(Const.KAFKA_TIMESTAMP_NAME)
    )
  }


  def postDstDataFrameToKafka(dstDF: DataFrame, config: Config): Unit = {
    val kafkaDstTopics = ConfigReader.getConfigField[String](config, "kafka.output.topic")
    val kafkaDstServers = ConfigReader.getConfigField[String](config, "kafka.output.bootstrap_servers")
    val aclDst = ConfigReader.getConfigField[Boolean](config, "kafka.output.acl")
    val checkpointPath = ConfigReader.getConfigField[String](config, "kafka.output.checkpoint_path")
    val securityProtocol = ConfigReader.getConfigField[String](config, "kafka.common.security_protocol")
    val saslMechanism = ConfigReader.getConfigField[String](config, "kafka.common.sasl_mechanism")
    val triggerInterval = ConfigReader.getConfigField[String](config, "kafka.trigger_interval")

    // Create a query writer to write to kafka
    var queryWriter: DataStreamWriter[Row] = dstDF.selectExpr("to_json(struct(*)) AS value")
      .writeStream
      .outputMode("append")
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaDstServers)
      .option("topic", kafkaDstTopics)
      .option("checkpointLocation", checkpointPath)
      .trigger(Trigger.ProcessingTime(triggerInterval))

    queryWriter = if (aclDst) {
      queryWriter.option("kafka.sasl.mechanism", saslMechanism)
        .option("kafka.security.protocol", securityProtocol)
    } else {
      queryWriter
    }

    while (true) {
      val query: StreamingQuery = queryWriter.start()
      if (config.hasPath("kafka.refresh_interval")) {
        val refreshIntervalMs = ConfigReader.getConfigField[Long](config, "kafka.refresh_interval")
        query.awaitTermination(refreshIntervalMs)
        logger.info(s"Restarting query after $refreshIntervalMs ms")
        query.stop()
      } else {
        query.awaitTermination()
      }
    }
  }


  def postDstDataFrameToConsole(dstDF: DataFrame, config: Config): Unit = {
    val triggerInterval = ConfigReader.getConfigField[String](config, "kafka.trigger_interval")

    val queryWriter = dstDF.writeStream
      .format("console")
      .outputMode("append")
      .option("truncate", "false")
      .trigger(Trigger.ProcessingTime(triggerInterval))

    while (true) {
      val query: StreamingQuery = queryWriter.start()
      if (config.hasPath("kafka.refresh_interval")) {
        val refreshIntervalMs = ConfigReader.getConfigField[Long](config, "kafka.refresh_interval")
        query.awaitTermination(refreshIntervalMs)
        logger.info(s"Restarting query after $refreshIntervalMs ms")
        query.stop()
      } else {
        query.awaitTermination()
      }
    }
  }

}