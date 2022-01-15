/* project-big-data-processing-pipeline
 *
 * Created: 11/1/22 3:21 pm
 *
 * Description:
 */
package streaming.job

import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, Row}
import org.scalatest.{FunSpec, GivenWhenThen}
import streaming.UnitTestRunner
import util.LoggerCreator

class TxnUserTest extends FunSpec with GivenWhenThen with UnitTestRunner {

  val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)

  val jobName = "TxnUser"

  val job: Job = {
    Class.forName(s"streaming.job.$jobName")
      .getConstructor()
      .newInstance()
      .asInstanceOf[Job]
  }
  job.registerUDF(spark)

  describe("Txn User logic") {
    it("should return correct output") {
      Given("a source dataframe with JSON having 'paid' order_status")
      val value =
        """
          |{
          |	"create_time": 1641885053,
          |	"currency": "PHP",
          |	"order_status": "paid",
          |	"user_id": 659334,
          |	"order_id": 7699329,
          |	"gmv": 3174.6346553637804,
          |	"event": {
          |		"database": "order"
          |	}
          |}
          |""".stripMargin
      val srcDF: DataFrame = getSourceDF(value)

      When("processed with job's logic")
      job.processRegisterInputTables(spark, srcDF)
      val dstDF: DataFrame = spark.sql(job.getSQLText)
      val rdd: Row = dstDF.rdd.collect()(0)

      Then("the returned output should be correct")
      assert(rdd.get(0) === 659334)
      assert(rdd.get(1) === "PH")
      assert(rdd.get(2) === 1641885053000L)
      assert(rdd.get(4) === "2022-01-11")
    }
  }

}