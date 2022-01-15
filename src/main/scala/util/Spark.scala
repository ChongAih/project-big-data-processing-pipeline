/* project-big-data-processing-pipeline
 *
 * Created: 1/1/22 8:50 pm
 *
 * Description:
 */
package util

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object Spark {

  var sparkSession: Option[SparkSession] = None

  def init(sparkConf: SparkConf): Unit = synchronized {
    sparkConf.set("spark.sql.crossJoin.enabled", "true")

    sparkSession = Some(SparkSession
      .builder
      .config(sparkConf)
      .enableHiveSupport()
      .getOrCreate())
  }

  def get(): SparkSession = {
    sparkSession match {
      case None => throw new RuntimeException("spark session has not been initialized, please invoke init method first")
      case Some(s) => s
    }
  }

}