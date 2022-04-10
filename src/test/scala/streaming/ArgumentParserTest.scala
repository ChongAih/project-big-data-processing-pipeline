package streaming

/* project-big-data-processing-pipeline
 *  
 * Created: 29/12/21 3:03 pm
 * 
 * Description: 
 */

import org.scalatest.{FunSpec, GivenWhenThen}

class ArgumentParserTest extends FunSpec with GivenWhenThen {

  describe("Scallop Argument Parser") {

    it("should return the user defined value correctly") {
      Given("an array of arguments")
      val arguments = Array("--job", "test",
        "--config-resource-path", "config/test.conf",
        "--kafka-start-time", "-1",
        "--kafka-end-time", "-1",
        "--local"
      )

      When("initialize an Scallop argument parser")
      val argumentParser = new ArgumentParser(arguments)

      Then("the argument values should be correct")
      assert(argumentParser.job.getOrElse("") === "test")
      assert(argumentParser.configResourcePath.getOrElse("") === "config/test.conf")
      assert(argumentParser.kafkaStartTime.getOrElse("") === "-1")
      assert(argumentParser.kafkaEndTime.getOrElse("") === "-1")
      assert(argumentParser.local.getOrElse(false) === true)
    }

  }

}