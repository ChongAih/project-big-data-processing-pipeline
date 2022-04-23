/* project-big-data-processing-pipeline
 *
 * Created: 20/3/22 1:00 pm
 *
 * Description:
 */
package udf

import org.apache.flink.table.functions.ScalarFunction

class FlinkSecond2MilliSecond extends ScalarFunction{
  def eval(time: Long): Long = {
    Second2MilliSecond(time)
  }
}

object FlinkSecond2MilliSecond {
  val name = Second2MilliSecond.name
}