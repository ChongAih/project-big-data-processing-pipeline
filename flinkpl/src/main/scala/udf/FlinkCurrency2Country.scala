/* project-big-data-processing-pipeline
 *
 * Created: 20/3/22 12:59 pm
 *
 * Description:
 */
package udf

import org.apache.flink.table.functions.ScalarFunction

class FlinkCurrency2Country extends ScalarFunction{
  def eval(currency: String): String = {
    Currency2Country(currency)
  }
}

object FlinkCurrency2Country {
  val name = Currency2Country.name
}