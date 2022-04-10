/* project-big-data-processing-pipeline
 *
 * Created: 1/2/22 8:03 pm
 *
 * Description:
 */
package util

import com.typesafe.config.Config
import org.scalatest.{FunSpec, GivenWhenThen}

class CaffeineCacheTest extends FunSpec with GivenWhenThen {

  class CCache extends CaffeineCache[String, String]

  describe("Caffeine Cache behaviour") {
    it("should cache data and return correct value if exists") {
      Given("a Caffeine Cache instance")
      val testKey = "key"
      val testValue = "value"
      val ccache = new CCache()

      When(s"caching with data with key: $testKey and value: $testValue")
      ccache.setCacheKeyData(testKey, testValue)

      Then(s"it should return correct $testValue when query key $testKey")
      val cacheValue = ccache.getCacheKeyData(testKey)
      assert(cacheValue.isDefined)
      assert(cacheValue.get === "value")

      Then(s"it should return nothing when query random key")
      val cacheValue2 = ccache.getCacheKeyData("xxxx")
      assert(cacheValue2.isEmpty)
    }
  }

}
