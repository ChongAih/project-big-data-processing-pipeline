package udf

import org.scalatest.{FunSpec, GivenWhenThen}

/* project-big-data-processing-pipeline
 *  
 * Created: 2/1/22 7:24 pm
 * 
 * Description: 
 */

class Currency2CountryTest extends FunSpec with GivenWhenThen {

  describe("Currency2Country UDF behaviour") {
    it("should return correct country based on currency") {
      Given("the Currency2Country UDF")

      When("checking for country mapping")

      Then("the returned mapped country should be correct")
      assert(Currency2Country("IDR") === "ID")
      assert(Currency2Country("PHP") === "PH")
      assert(Currency2Country("MYR") === "MY")
      assert(Currency2Country("SGD") === "SG")

      And("the \"\" should be returned if no mapping is found")
      assert(Currency2Country("xxx").isEmpty)
    }
  }

}