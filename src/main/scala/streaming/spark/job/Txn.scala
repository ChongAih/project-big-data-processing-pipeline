/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 9:06 pm
 *
 * Description:
 */
package streaming.spark.job

import com.typesafe.config.Config
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, Encoder, Encoders, Row, SparkSession}
import streaming.{Order, Value}
import udf.{Currency2Country, ExchangeRate, Second2MilliSecond}
import util.{Const, JacksonScalaObjectMapper, LoggerCreator}

class TxnJacksonScalaObjectMapper extends JacksonScalaObjectMapper

class Txn extends Job {
  @transient lazy val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)

  val order_table: String = "order"

  override def registerUDF(sparkSession: SparkSession): Unit = {
    sparkSession.udf.register(Currency2Country.name, Currency2Country)
    sparkSession.udf.register(Second2MilliSecond.name, Second2MilliSecond)
    sparkSession.udf.register(ExchangeRate.name, ExchangeRate)
  }

  override def getSQLText: String = {
    s"""
       |WITH orders AS (
       |  SELECT
       |    gmv,
       |    order_id,
       |    user_id,
       |    ${Currency2Country.name}(currency) AS country,
       |    ${Second2MilliSecond.name}(create_time) AS ${getEventTimeName},
       |    ${Second2MilliSecond.name}(UNIX_TIMESTAMP(CURRENT_TIMESTAMP)) AS processing_time
       |  FROM $order_table
       |  WHERE order_status = 'paid'
       |)
       |SELECT
       |  *,
       |  gmv * ${ExchangeRate.name}(country, CAST(DATE(current_timestamp) AS STRING)) AS gmv_usd
       |FROM orders
       |""".stripMargin
  }

  override def getDuplicationFieldNames: List[String] = List("country", "order_id")

  // Since the task is run in different executors,
  // thus some initialization of object/ instance/ variables has to be done in each executor
  override def initializeSettingInExecutors(config: Config): Unit = {
    ExchangeRate.initialize(config)
  }

  override def processRegisterInputTables(config: Config, sparkSession: SparkSession,
                                          sourceDF: DataFrame): Unit = {

    // Create a customized de/serialization encoder as it is not available in spark.implicits._
    // Must be a class class or subtype of Product
    implicit val txnInputEncoder: Encoder[Order] = Encoders.product[Order]

    // Create only a object mapper for each partition
    val df = sourceDF.mapPartitions(rows => {
      // Initialize setting in each executor/ partition
      initializeSettingInExecutors(config)
      val txnJacksonScalaObjectMapper = new TxnJacksonScalaObjectMapper()
      rows.flatMap(row => {
        try {
          val valueJson = row.getAs[String](Const.KAFKA_VALUE_NAME)
          val valueInput = txnJacksonScalaObjectMapper.deserialize[Value](valueJson, classOf[Value])
          if (valueInput.event.isDefined && valueInput.event.get.database == "order") {
            List(txnJacksonScalaObjectMapper.deserialize[Order](valueJson, classOf[Order]))
          } else {
            List()
          }
        } catch {
          case e: Exception => {
            logger.error(e) // logger info appears in executor logs
            List(Order(0, "MYR", "paid"))
          }
        }
      })
    })

    // watermark is needed when there is a need for streaming join
    // or time based computation (past 5 minutes order count)
    df.selectExpr("*", "CAST(create_time AS timestamp) AS event_timestamp")
      .withWatermark("event_timestamp", "1440 minutes")
      .createOrReplaceTempView(order_table)
  }

}