/* project-big-data-processing-pipeline
 *
 * Created: 2/1/22 8:09 pm
 *
 * Description:
 */
package streaming.job


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


case class Order(create_time: Long = 0L, currency: String = "",
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