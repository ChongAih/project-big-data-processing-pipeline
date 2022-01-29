/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 7:29 pm
 *
 * Description:
 */
package util

import org.apache.spark.sql.Row
import util.ConversionHelper.getMilliSecond

import java.time.ZoneId
import java.util.{Calendar, Date}

class EventTimeFilter(eventTimeFieldName: String) extends ((Row) => Boolean) with Serializable {

  val defaultTimeZone: ZoneId = ZoneId.systemDefault

  override def apply(row: Row): Boolean = {
    val eventTime = getMilliSecond(row.getAs(eventTimeFieldName).toString.toLong)
    checkWithinProcessingWindow(eventTime)
  }

  def checkWithinProcessingWindow(eventTime: Long): Boolean = {
    val cal: Calendar = Calendar.getInstance()
    val todayDate: Date = Date.from(java.time.LocalDate.now().atStartOfDay(defaultTimeZone).toInstant)
    val todayStartTime = {
      cal.setTime(todayDate)
      getMilliSecond(cal.getTime.getTime)
    }
    val tmrStartTime = {
      cal.setTime(todayDate)
      cal.add(Calendar.DATE, 1)
      getMilliSecond(cal.getTime.getTime)
    }
    (eventTime >= todayStartTime) && (eventTime < tmrStartTime)
  }

}