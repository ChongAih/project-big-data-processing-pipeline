/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 9:06 pm
 *
 * Description:
 */
package streaming.job

import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, Encoder, Encoders, SparkSession}
import udf.{Currency2Country, Second2MilliSecond}
import util.{Const, JacksonScalaObjectMapper, LoggerCreator}

class TxnJacksonScalaObjectMapper extends JacksonScalaObjectMapper

class Txn extends Job {
  @transient lazy val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)

  val order_table: String = "order"

  override def registerUDF(sparkSession: SparkSession): Unit = {
    sparkSession.udf.register(Currency2Country.name, Currency2Country)
    sparkSession.udf.register(Second2MilliSecond.name, Second2MilliSecond)
  }

  override def getSQLText: String = {
    s"""
       |SELECT
       |  gmv,
       |  order_id,
       |  user_id,
       |  ${Currency2Country.name}(currency) AS country,
       |  ${Second2MilliSecond.name}(create_time) AS event_time,
       |  ${Second2MilliSecond.name}(UNIX_TIMESTAMP(CURRENT_TIMESTAMP)) AS processing_time
       |FROM $order_table
       |WHERE order_status = 'paid'
       |""".stripMargin
  }

  override def getDuplicationFieldNames: List[String] = List("country", "order_id", "event_date")

  override def processRegisterInputTables(sparkSession: SparkSession,
                                          sourceDF: DataFrame): Unit = {

    // Create a customized de/serialization encoder as it is not available in spark.implicits._
    // Must be a class class or subtype of Product
    implicit val txnInputEncoder: Encoder[Order] = Encoders.product[Order]

    // Create only a object mapper for each partition
    val df = sourceDF.mapPartitions(rows => {
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
            logger.error(e)
            List(Order(0, "MYR", "paid"))
          }
        }
      })
    })

    df.createOrReplaceTempView(order_table)
  }

}