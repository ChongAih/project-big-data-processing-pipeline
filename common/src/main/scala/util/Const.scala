/* project-big-data-processing-pipeline
 *
 * Created: 29/12/21 4:37 pm
 *
 * Description:
 */
package util

object Const {
  val FALLBACK_CONFIG_RESOURCE_PATH = "config/defaults.conf"
  val KAFKA_DEFAULT_LATEST_OFFSET = "-1"
  val KAFKA_DEFAULT_EARLIEST_OFFSET = "-2"
  val SPARK_LOCAL_MASTER = true
  val KAFKA_VALUE_NAME = "value"
  val KAFKA_TIMESTAMP_NAME = "timestamp"
  val MILLISECOND_MASK = 0xFFFFF00000000000L
  val SECOND_MASK = 0xFFFFFFFF00000000L
  val CAFFEINE_MAX_CACHE_SIZE = 1000
  val CAFFEINE_INIT_CACHE_SIZE = 100
  val CAFFEINE_EXPIRE_AFTER_ACCESS = 60
  val CAFFEINE_EXPIRE_AFTER_WRITE = 60
  val CAFFEINE_ENABLE_RECORD_STAT = true
  val TIMEZONE = "Asia/Singapore"
}