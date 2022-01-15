/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 9:07 pm
 *
 * Description:
 */
package streaming.job

import org.apache.spark.sql.{DataFrame, SparkSession}

abstract class Job extends Serializable {
  def registerUDF(sparkSession: SparkSession): Unit = {}

  def getSQLText: String

  def getDuplicationFieldNames: List[String]

  def processRegisterInputTables(sparkSession: SparkSession, sourceDF: DataFrame): Unit
}