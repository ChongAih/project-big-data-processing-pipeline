/* project-big-data-processing-pipeline
 *
 * Created: 15/1/22 8:27 pm
 *
 * Description:
 */
package streaming

// Test and view output when Kafka cluster is ready
object StreamingRunnerTest extends StreamingRunnerHelper {

  def main(args: Array[String]): Unit = {
    run(Array("--job", "Txn",
      "--config-resource-path", "txn_local.conf",
      "--kafka-start-time", "1642157787238",
      "--kafka-end-time", "-1",
      "--spark-local-master"))
  }

}