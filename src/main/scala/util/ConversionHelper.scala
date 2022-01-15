/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 7:11 pm
 *
 * Description:
 */
package util

object ConversionHelper {
  def getMilliSecond(time: Long): Long = {
    if ((time & Const.SECOND_MASK) != 0) time else time * 1000L
  }
}