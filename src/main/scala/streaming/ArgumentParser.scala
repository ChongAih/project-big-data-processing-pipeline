/* project-big-data-processing-pipeline
 *
 * Created: 29/12/21 2:43 pm
 *
 * Description:
 */
package streaming

import org.rogach.scallop.{ScallopConf, ScallopOption}

class ArgumentParser(args: Array[String]) extends ScallopConf(args) {

  val job: ScallopOption[String] = opt[String]("job", required = true, descr = "job class name to be run")
  val configResourcePath: ScallopOption[String] = opt[String]()
  val kafkaStartTime: ScallopOption[String] = opt[String]()
  val kafkaEndTime: ScallopOption[String] = opt[String]()
  val local: ScallopOption[Boolean] = opt[Boolean]()

  verify()
}
