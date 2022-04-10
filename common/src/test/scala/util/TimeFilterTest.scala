/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 8:00 pm
 *
 * Description:
 */
package util

import org.scalatest.{FunSpec, GivenWhenThen}

import util.TimeFilter.checkWithinProcessingWindow

class TimeFilterTest extends FunSpec with GivenWhenThen {

  describe("EventTimeFilter behaviour") {
    it("should return filter data based on event time") {
      Given("an event time filter")

      When("tested with different event time")

      Then("data should be filter correctly based on event time")
      assert(checkWithinProcessingWindow(System.currentTimeMillis()))
      assert(!checkWithinProcessingWindow(System.currentTimeMillis()-(3600L*24L*1000L)))
    }
  }

}