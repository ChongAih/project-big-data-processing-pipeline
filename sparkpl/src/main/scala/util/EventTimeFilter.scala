/* project-big-data-processing-pipeline
 *
 * Created: 10/4/22 8:14 pm
 *
 * Description:
 */
package util

import org.apache.spark.sql.Row
import util.ConversionHelper.getMilliSecond

class EventTimeFilter(eventTimeFieldName: String) extends (Row => Boolean) with Serializable {

  import TimeFilter._

  override def apply(row: Row): Boolean = {
    val eventTime = getMilliSecond(row.getAs(eventTimeFieldName).toString.toLong)
    checkWithinProcessingWindow(eventTime)
  }

}