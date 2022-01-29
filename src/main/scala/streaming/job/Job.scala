/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 9:07 pm
 *
 * Description:
 */
package streaming.job

import com.typesafe.config.Config
import org.apache.spark.sql.{DataFrame, SparkSession}
import util.{DeduplicationFilter, EventTimeFilter}

abstract class Job extends Serializable {

  def registerUDF(sparkSession: SparkSession): Unit = {}

  def getSQLText: String

  def processRegisterInputTables(sparkSession: SparkSession, sourceDF: DataFrame): Unit

  final def deduplicationAndTimeFilter(df: DataFrame, config: Config): DataFrame = {
    df.filter(new EventTimeFilter(getEventTimeName))
      .filter(new DeduplicationFilter(config, getDuplicationFieldNames))
  }

  def getDuplicationFieldNames: List[String]

  final def getEventTimeName: String = "event_time"

}