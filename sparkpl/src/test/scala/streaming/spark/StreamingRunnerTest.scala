/* project-big-data-processing-pipeline
 *
 * Created: 10/2/22 10:59 am
 *
 * Description:
 */
package streaming.spark

// Test and view output when Kafka cluster is ready
object StreamingRunnerTest extends StreamingRunnerHelper {

  // Only work in a Kafka without ACL locally
  def main(args: Array[String]): Unit = {
    run(Array("--job", "Txn",
      "--config-resource-path", "txn_local.conf",
      "--kafka-start-time", "1642157787238",
      "--kafka-end-time", "-1",
      "--local"))
  }

}