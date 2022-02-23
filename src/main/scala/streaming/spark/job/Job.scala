/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 9:07 pm
 *
 * Description:
 */
package streaming.spark.job

import com.typesafe.config.Config
import org.apache.spark.sql.{DataFrame, SparkSession}
import util.{DeduplicationFilter, EventTimeFilter}

abstract class Job extends Serializable {

  def registerUDF(sparkSession: SparkSession): Unit = {}

  def getSQLText: String

  // Since the task is run in different executors,
  // thus some initialization of object/ instance/ variables have to be done in each executor
  def initializeSettingInExecutors(config: Config): Unit = {}

  def processRegisterInputTables(config: Config, sparkSession: SparkSession, sourceDF: DataFrame): Unit

  final def deduplicationAndTimeFilter(df: DataFrame, config: Config): DataFrame = {
    df.filter(new EventTimeFilter(getEventTimeName))
      .filter(new DeduplicationFilter(config, getDuplicationFieldNames))
  }

  def getDuplicationFieldNames: List[String]

  final def getEventTimeName: String = "event_time"

}