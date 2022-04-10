/* project-big-data-processing-pipeline
 *
 * Created: 20/3/22 9:12 am
 *
 * Description:
 */
package streaming.flink.processor

import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.scalatest.{FunSpec, GivenWhenThen}
import streaming.KafkaOutputData


class FlinkDeduplicationFilterTest extends FunSpec with GivenWhenThen {
  describe("FlinkDeduplicationFilter") {
    it("should return remove duplicated item based on the given keys within the given period") {
      Given("Flink environment")
      val senv: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

      When("ingesting a set of duplicated data")
      val input: DataStream[KafkaOutputData] = senv.fromElements(
        KafkaOutputData(1, "123", "rowkey"),
        KafkaOutputData(1, "123", "rowkey"),
        KafkaOutputData(1, "123", "rowkey")
      )
      val output = {
        input.keyBy(item => {item.rowkey})
          .flatMap(new FlinkDeduplicationFilter)
          .executeAndCollect()
      }

      Then("the duplicated data should be removed")
      assert(output.size == 1)
    }
  }
}