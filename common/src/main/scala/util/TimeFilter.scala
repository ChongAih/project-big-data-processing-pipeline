/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 7:29 pm
 *
 * Description:
 */
package util

import util.ConversionHelper.getMilliSecond

import java.time.ZoneId
import java.util.{Calendar, Date}

object TimeFilter {

  val defaultTimeZone: ZoneId = ZoneId.systemDefault

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