/* project-big-data-processing-pipeline
 *
 * Created: 3/1/22 8:07 pm
 *
 * Description:
 */
package udf

import util.ConversionHelper

object Second2MilliSecond extends (Long => Long) with Serializable {
  val name: String = "second_to_millisecond"

  override def apply(time: Long): Long = {
    ConversionHelper.getMilliSecond(time)
  }
}