/* project-big-data-processing-pipeline
 *
 * Created: 3/1/22 8:55 pm
 *
 * Description:
 */
package streaming.job

import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, Encoder, Encoders, SparkSession}
import udf.{Currency2Country, Second2MilliSecond}
import util.{Const, JacksonScalaObjectMapper, LoggerCreator}


class TxnUser extends Job {
  @transient lazy val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)

  val order_table: String = "order"

  override def registerUDF(sparkSession: SparkSession): Unit = {
    sparkSession.udf.register(Currency2Country.name, Currency2Country)
    sparkSession.udf.register(Second2MilliSecond.name, Second2MilliSecond)
  }

  override def getSQLText: String = {
    s"""
       |WITH order AS (
       |  SELECT
       |    gmv,
       |    order_id,
       |    user_id,
       |    ${Currency2Country.name}(currency) AS country,
       |    ${Second2MilliSecond.name}(create_time) AS event_time,
       |    ${Second2MilliSecond.name}(UNIX_TIMESTAMP(CURRENT_TIMESTAMP)) AS processing_time
       |  FROM $order_table
       |  WHERE order_status = 'paid'
       |)
       |SELECT
       |  user_id,
       |  country,
       |  event_time,
       |  processing_time,
       |  CAST(DATE(FROM_UNIXTIME(event_time/1000)) AS STRING) AS event_date
       |FROM order
       |""".stripMargin
  }

  override def getDuplicationFieldNames: List[String] = List("country", "user_id", "event_date")

  override def processRegisterInputTables(sparkSession: SparkSession,
                                          sourceDF: DataFrame): Unit = {

    // Create a customized de/serialization encoder as it is not available in spark.implicits._
    // Must be a class class or subtype of Product
    implicit val txnInputEncoder: Encoder[Order] = Encoders.product[Order]

    // Create only a object mapper for each partition
    val df = sourceDF.mapPartitions(rows => {
      val txnUserJacksonScalaObjectMapper = new TxnUserJacksonScalaObjectMapper()
      rows.flatMap(row => {
        try {
          val valueJson = row.getAs[String](Const.KAFKA_VALUE_NAME)
          val valueInput = txnUserJacksonScalaObjectMapper.deserialize[Value](valueJson, classOf[Value])
          if (valueInput.event.isDefined && valueInput.event.get.database == "order") {
            List(txnUserJacksonScalaObjectMapper.deserialize[Order](valueJson, classOf[Order]))
          } else {
            List()
          }
        } catch {
          case e: Exception => {
            logger.error(e)
            List()
          }
        }
      })
    })

    df.createOrReplaceTempView(order_table)
  }

}

class TxnUserJacksonScalaObjectMapper extends JacksonScalaObjectMapper