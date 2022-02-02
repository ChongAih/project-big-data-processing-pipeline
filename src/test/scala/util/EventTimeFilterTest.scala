/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 8:00 pm
 *
 * Description:
 */
package util

import org.scalatest.{FunSpec, GivenWhenThen}

class EventTimeFilterTest extends FunSpec with GivenWhenThen {

  describe("EventTimeFilter behaviour") {
    it("should return filter data based on event time") {
      Given("an event time filter")
      val eventTimeFilter = new EventTimeFilter("")

      When("tested with different event time")

      Then("data should be filter correctly based on event time")
      assert(eventTimeFilter.checkWithinProcessingWindow(System.currentTimeMillis()))
      assert(!eventTimeFilter.checkWithinProcessingWindow(System.currentTimeMillis()-(3600L*24L*1000L)))
    }
  }

}