/* project-big-data-processing-pipeline
 *
 * Created: 15/3/22 7:27 pm
 *
 * Description:
 */
package streaming.flink

import com.typesafe.config.Config
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend
import org.apache.flink.runtime.state.storage.JobManagerCheckpointStorage
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.EnvironmentSettings

import java.time.Duration
import java.time.temporal
import java.util
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters.asScalaSetConverter


object FlinkTableConfigType {
  val STRING = 1
  val INT = 2
  val BOOLEAN = 3
  val mapping: util.Map[String, Int] = {
    val temp: util.Map[String, Int] = new util.HashMap()
    temp.put("table.exec.mini-batch.enabled", STRING)
    temp.put("table.exec.mini-batch.allow-latency", STRING)
    temp.put("table.exec.mini-batch.size", STRING)
    temp
  }
}


trait FlinkSettingHelper {

  def getFlinkStreamExecutionEnv(flinkConfig: Config): StreamExecutionEnvironment = {
    val senv: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    setStateBackendConfig(senv, flinkConfig)
    setCheckpointConfig(senv, flinkConfig)
    setExecutionConfig(senv, flinkConfig)
    senv
  }


  def setStateBackendConfig(senv: StreamExecutionEnvironment, flinkConfig: Config): Unit = {
    val stateBackend = {
      if (flinkConfig.hasPath("state_backend.name")) {
        flinkConfig.getString("state_backend.name")
      } else {
        "memory"
      }
    }
    stateBackend match {
      case "memory" => {
        senv.setStateBackend(new HashMapStateBackend)
        senv.getCheckpointConfig.setCheckpointStorage(new JobManagerCheckpointStorage)
      }
      case "filesystem" => {
        val checkpointDir: String = flinkConfig.getString("state_backend.checkpoint")
        senv.setStateBackend(new HashMapStateBackend)
        senv.getCheckpointConfig.setCheckpointStorage(checkpointDir)
      }
      case "rocksdb" => {
        val checkpointDir: String = flinkConfig.getString("state_backend.checkpoint")
        senv.setStateBackend(new EmbeddedRocksDBStateBackend)
        senv.getCheckpointConfig.setCheckpointStorage(checkpointDir)
      }
    }
  }


  def setCheckpointConfig(senv: StreamExecutionEnvironment, flinkConfig: Config): Unit = {
    if (flinkConfig.hasPath("checkpoint")) {
      val checkpointInterval = flinkConfig.getLong("checkpoint.interval")
      val checkpointTimeout = flinkConfig.getLong("checkpoint.timeout")
      val checkpointMode = flinkConfig.getString("checkpoint.mode")
      val checkpointPause = flinkConfig.getLong("checkpoint.pause")
      val checkpointTolerableFailure = flinkConfig.getInt("checkpoint.tolerable-failure")
      val checkpointMaxConcurrency = flinkConfig.getInt("checkpoint.max-concurrent-checkpoints")

      senv.enableCheckpointing(checkpointInterval)
      val checkpointConfig = senv.getCheckpointConfig
      checkpointConfig.setCheckpointTimeout(checkpointTimeout)
      checkpointMode match {
        case "exactly-once" => checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
        case "at-least-once" => checkpointConfig.setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE)
        case _ => checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
      }
      checkpointConfig.setMinPauseBetweenCheckpoints(checkpointPause)
      checkpointConfig.setTolerableCheckpointFailureNumber(checkpointTolerableFailure)
      checkpointConfig.setMaxConcurrentCheckpoints(checkpointMaxConcurrency)
    }
  }


  def setExecutionConfig(senv: StreamExecutionEnvironment, flinkConfig: Config): Unit = {
    // note that parallelism (all operators) must be smaller than max-parallelism (key operators)
    val maxParallelism = {
      if (flinkConfig.hasPath("execution.max-parallelism")) {
        flinkConfig.getInt("execution.max-parallelism")
      } else 128
    }
    senv.setMaxParallelism(maxParallelism)

    if (flinkConfig.hasPath("execution.restart-strategy")) {
      val restartType = flinkConfig.getString("execution.restart-strategy.type")
      restartType match {
        case "fixed-delay-restart" => {
          val restartAttempts = flinkConfig.getInt("execution.restart-strategy.restart-attempts")
          val delayBetweenAttempts = flinkConfig.getInt("execution.restart-strategy.delay-between-attempts")
          senv.setRestartStrategy(RestartStrategies.fixedDelayRestart(
            restartAttempts, delayBetweenAttempts
          ))
        }
        case "exponential-delay-restart" => {
          val maxDelayBetweenAttempts = flinkConfig.getInt("execution.restart-strategy.max-delay-between-attempts")
          val exponentialMultiplier = flinkConfig.getInt("execution.restart-strategy.exponential-multiplier")
          senv.setRestartStrategy(RestartStrategies.exponentialDelayRestart(
            Time.of(1, TimeUnit.MILLISECONDS),
            Time.of(maxDelayBetweenAttempts, TimeUnit.MILLISECONDS),
            exponentialMultiplier,
            Time.of(2, TimeUnit.SECONDS),
            0.1
          ))
        }
        case "failure-rate-restart" => {
          val maxFailuresPerUnit = flinkConfig.getInt("execution.restart-strategy.max-failures")
          val intervalBetweenMeasurement = flinkConfig.getInt("execution.restart-strategy.interval")
          val delayBetweenAttempts = flinkConfig.getInt("execution.restart-strategy.delay-between-attempts")
          senv.setRestartStrategy(RestartStrategies.failureRateRestart(
            maxFailuresPerUnit,
            Time.of(intervalBetweenMeasurement, TimeUnit.MINUTES),
            Time.of(delayBetweenAttempts, TimeUnit.SECONDS)
          ))
        }
        case "no-restart" => {
          senv.setRestartStrategy(RestartStrategies.noRestart())
        }
      }
    }
  }


  def getFlinkStreamTableEnv(senv: StreamExecutionEnvironment, flinkConfig: Config): StreamTableEnvironment = {
    // Build streaming blink planner table env
    val settings: EnvironmentSettings = EnvironmentSettings
      .newInstance()
      .inStreamingMode()
      .build()
    val tenv: StreamTableEnvironment = StreamTableEnvironment.create(senv, settings)

    if (flinkConfig.hasPath("table.idle-state-retention")) {
      val idleStateRetention = Duration.of(
        flinkConfig.getLong("table.idle-state-retention"),
        temporal.ChronoUnit.HOURS
      )
      tenv.getConfig.setIdleStateRetention(idleStateRetention)
    }

    // Update table configuration
    val tableConfiguration = tenv.getConfig.getConfiguration
    tableConfiguration.setString("table.dynamic-table-options.enabled", "true") // to allow SQL hint option
    if (flinkConfig.hasPath("table.configuration")) {
      val tableFlinkConfig = flinkConfig.getConfig("table.configuration")
      tableFlinkConfig.entrySet().asScala.foreach(item => {
        val key = "table" + "." + item.getKey
        val value = tableFlinkConfig.getString(item.getKey)
        if (FlinkTableConfigType.mapping.containsKey(key)) {
          val mappingType = FlinkTableConfigType.mapping.get(key)
          mappingType match {
            case FlinkTableConfigType.STRING => tableConfiguration.setString(key, value)
            case FlinkTableConfigType.INT => tableConfiguration.setInteger(key, value.toInt)
            case FlinkTableConfigType.BOOLEAN => tableConfiguration.setBoolean(key, value.toBoolean)
          }
        }
      })
    }

    // TODO: Hive table registration

    tenv
  }

}