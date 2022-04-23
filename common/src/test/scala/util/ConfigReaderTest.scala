package util

/* project-big-data-processing-pipeline
 *  
 * Created: 28/12/21 7:35 pm
 * 
 * Description: 
 */

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FunSpec, GivenWhenThen}
import util.ConfigReader.defaultConfig

class ConfigReaderTest extends FunSpec with GivenWhenThen {

  describe("Configuration reader behaviour") {
    it("should read configuration file from resource folder and " +
      "return user-defined values if exists else default values") {
      Given("a default and a user-defined configuration files")

      When("reading test.conf")
      val config: Config = ConfigReader.readConfig("config/test.conf")

      Then("the available user-defined values should be correct")
      assert(ConfigReader.getConfigField[String](config,
        "kafka.input.bootstrap_servers") === "localhost")
      assert(ConfigReader.getConfigField[String](config,
        "kafka.input.topic") === "test_chongaih_src")
      assert(ConfigReader.getConfigField[Boolean](config,
        "kafka.acl") === false)
      assert(ConfigReader.getConfigField[Int](config,
        "hbase.port") === 2181)

      And("the field should be set as default value if not set by user")
      assert(ConfigReader.getConfigField[String](config,
        "kafka.common.sasl_mechanism") === "PLAIN")

      And("the exception should be thrown if any required field is missing")
      assertThrows[Exception] {
        ConfigReader.getConfigField[String](config, "missing.field")
      }
    }
  }

}
