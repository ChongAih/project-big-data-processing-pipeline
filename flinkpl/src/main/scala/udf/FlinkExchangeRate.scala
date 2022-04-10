/* project-big-data-processing-pipeline
 *
 * Created: 21/3/22 6:55 pm
 *
 * Description:
 */
package udf

import com.typesafe.config.Config
import org.apache.flink.table.functions.{FunctionContext, ScalarFunction}

class FlinkExchangeRate(config: Config) extends ScalarFunction{

  override def open(context: FunctionContext): Unit = {
    ExchangeRate.initialize(config)
    super.open(context)
  }

  def eval(country: String, date: String): Double = {
    val temp = ExchangeRate(country, date)
    temp.toString.toDouble
  }
}

object FlinkExchangeRate {
  val name = ExchangeRate.name
}