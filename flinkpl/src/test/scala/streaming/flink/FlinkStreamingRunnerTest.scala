/* project-big-data-processing-pipeline
 *
 * Created: 23/3/22 7:30 am
 *
 * Description:
 */
package streaming.flink

object FlinkStreamingRunnerTest extends FlinkStreamingRunnerHelper {
  def main(args: Array[String]): Unit = {
    run(Array("--job", "FlinkTxn",
      "--config-resource-path", "flink_txn_local.conf",
      "--kafka-start-time", "-1",
      "--kafka-end-time", "-1",
      "--local"))
  }
}