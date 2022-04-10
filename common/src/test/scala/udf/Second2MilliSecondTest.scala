/* project-big-data-processing-pipeline
 *
 * Created: 14/1/22 7:00 pm
 *
 * Description:
 */
package udf

import org.scalatest.{FunSpec, GivenWhenThen}

class Second2MilliSecondTest extends FunSpec with GivenWhenThen {

  describe("Second2MilliSecond UDF behaviour") {
    it("should return correct millisecond value") {
      Given("the Second2MilliSecond UDF")

      When("converting any value to millisecond")

      Then("the second input should be converted")
      assert(Second2MilliSecond(1642157787L) === 1642157787000L)

      And("the millisecond input should be unchanged")
      assert(Second2MilliSecond(1642157787238L) === 1642157787238L)
    }
  }

}