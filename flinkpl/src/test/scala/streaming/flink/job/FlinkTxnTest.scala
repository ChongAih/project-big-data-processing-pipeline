/* project-big-data-processing-pipeline
 *
 * Created: 21/4/22 2:18 pm
 *
 * Description:
 */
package streaming.flink.job

import org.apache.flink.streaming.api.scala.createTypeInformation
import org.scalatest.{FunSpec, GivenWhenThen}
import streaming.flink.FlinkUnitTestRunner
import streaming.TxnOutput


class FlinkTxnTest extends FunSpec with GivenWhenThen {

  val jobName = "FlinkTxn"
  val configResourcePath = "flink_txn_local.conf"

  def initializeTester(): FlinkUnitTestRunner = {
    val tester = new FlinkUnitTestRunner()
    tester.createJob(jobName)
    tester.registerUDF(configResourcePath)
    tester
  }

  describe("Flink Txn logic") {
    val tester = initializeTester()

    it("should return correct output") {
      Given("a source dataframe with JSON having 'paid' order_status")
      val json =
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

      val resultIt = tester.processJson[TxnOutput](false, json)
      val item = resultIt.next()

      Then("the returned output should be correct")
      assert(item.gmv === 3174.6346553637804)
      assert(item.order_id === 7699329)
      assert(item.user_id === 659334)
      assert(item.country === "PH")
      assert(item.event_time === 1641885053000L)
      assert(item.gmv_usd === 0.0d)
    }

    it("should return nothing") {
      val tester = initializeTester()

      Given("a source dataframe with expired data")
      val json =
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
      val resultIt = tester.processJson[TxnOutput](true, json)

      Then("the returned output should be empty")
      assert(resultIt.isEmpty)
    }

    it("should remove duplicated data") {
      val tester = initializeTester()

      Given("a source dataframe with duplicated data")
      val json =
        s"""
          |{
          |	"create_time": ${(System.currentTimeMillis()/1000).toInt},
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
      val json2 =
        s"""
           |{
           |	"create_time": ${(System.currentTimeMillis()/1000-5000).toInt},
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
      val resultIt = tester.processJson[TxnOutput](true, json, json2)

      Then("the returned output should be empty")
      assert(resultIt.size==1)
    }
  }
}