/* project-big-data-processing-pipeline
 *
 * Created: 23/3/22 7:30 am
 *
 * Description:
 */
package streaming.flink

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment

object FlinkStreamingRunnerTest extends FlinkStreamingRunnerHelper {
  def main(args: Array[String]): Unit = {
    val senv: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val settings: EnvironmentSettings = EnvironmentSettings
      .newInstance()
      .inStreamingMode()
      .build()
    StreamTableEnvironment.create(senv, settings)

//    run(Array("--job", "FlinkTxn",
//      "--config-resource-path", "flink_txn_local.conf",
//      "--kafka-start-time", "-1",
//      "--kafka-end-time", "-1"))
  }
}