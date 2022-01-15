/* project-big-data-processing-pipeline
 *
 * Created: 11/1/22 3:19 pm
 *
 * Description:
 */
package streaming

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import util.Const

trait UnitTestRunner {

  val defaultSparkConf: SparkConf = new SparkConf()
    .setAppName("Unit-Test-Runner")
    .setMaster("local[6]")
  val spark: SparkSession = getSparkSession(defaultSparkConf)

  import spark.implicits._

  def getSparkSession(sparkConf: SparkConf = defaultSparkConf): SparkSession = {
    SparkSession
      .builder
      .config(sparkConf)
      .enableHiveSupport()
      .getOrCreate()
  }

  def getSourceDF(json: String): DataFrame = {
    spark.sparkContext.parallelize(Seq(json)).toDF(Const.KAFKA_VALUE_NAME)
  }
}