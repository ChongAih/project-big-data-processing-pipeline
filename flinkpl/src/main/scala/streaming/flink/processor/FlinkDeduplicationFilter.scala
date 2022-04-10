/* project-big-data-processing-pipeline
 *
 * Created: 20/3/22 8:46 am
 *
 * Description:
 */
package streaming.flink.processor

import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{StateTtlConfig, ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector
import streaming.KafkaOutputData

// Deduplication filter based on key
class FlinkDeduplicationFilter extends RichFlatMapFunction[KafkaOutputData, KafkaOutputData]{
  private var keepState: ValueState[Boolean] = _

  override def open(parameters: Configuration): Unit = {
    val ttlConfig: StateTtlConfig = StateTtlConfig.newBuilder(
      Time.days(1))
      .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
      .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
      .cleanupIncrementally(100, true)
      .build()
    val valueSD = new ValueStateDescriptor[Boolean]("flag", classOf[Boolean])
    valueSD.enableTimeToLive(ttlConfig)
    keepState = getRuntimeContext.getState(valueSD)
  }

  override def flatMap(in: KafkaOutputData, collector: Collector[KafkaOutputData]): Unit = {
    if (!keepState.value()) {
      collector.collect(in)
      keepState.update(true)
    }
  }
}