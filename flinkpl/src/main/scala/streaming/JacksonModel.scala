/* project-big-data-processing-pipeline
 *
 * Created: 2/1/22 8:09 pm
 *
 * Description:
 */
package streaming

import scala.annotation.StaticAnnotation


final class eventTimeAnnotation(isEventTime: Boolean) extends StaticAnnotation


case class Value(value: String = "", event: Option[Event] = None) {
  override def toString: String = {
    s"{value: $value; event: $event}"
  }
}


case class Event(database: String = "") {
  override def toString: String = {
    s"{database: $database}"
  }
}


case class Order(@eventTimeAnnotation(isEventTime = true) create_time: Long = 0L, currency: String = "",
                 order_status: String = "", user_id: Long = 0L,
                 order_id: Long = 0L, gmv: Double = 0d) {
  override def toString: String = {
    s"""
       |{
       |  create_time: $create_time, currency: $currency,
       |  order_status: $order_status, user_id: $user_id,
       |  order_id: $order_id, gmv: $gmv
       |}""".stripMargin
  }
}


case class KafkaInputData(kafkaValue: String = "", kafkaTime: Long) {
  override def toString: String = {
    s"""
       |{
       |  kafkaValue: $kafkaValue,
       |  kafkaTime: $kafkaTime
       |}""".stripMargin
  }
}


case class KafkaOutputData(eventTime: Long, kafkaValue: String = "",
                           rowkey: String = "", hbaseRowkey: String = "")


case class TxnOutput(gmv: Double, order_id: Long, user_id: Long,
                     country: String, event_time: Long,
                     processing_time: Long, gmv_usd: Double)


case class TxnUserOutput(gmv: Double, order_id: Long, user_id: Long,
                         country: String, event_time: Long,
                         processing_time: Long, gmv_usd: Double)
