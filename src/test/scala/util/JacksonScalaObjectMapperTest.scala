package util

/* project-big-data-processing-pipeline
 *  
 * Created: 28/12/21 9:43 am
 * 
 * Description: 
 */

import com.fasterxml.jackson.annotation.JsonProperty
import org.scalatest.{FunSpec, GivenWhenThen}

import scala.beans.BeanProperty

class JacksonScalaObjectMapperTest extends FunSpec with GivenWhenThen {

  var objectMapper: JacksonScalaObjectMapper = _

  def initializeDefaultJacksonScalaObjectMapper(): Unit = {
    objectMapper = new JacksonScalaObjectMapper()
  }

  def initializeCustomizedJacksonScalaObjectMapper(): Unit = {
    objectMapper = new CustomizedJacksonScalaObjectMapper()
  }

  describe("Default Jackson Scala Object Mapper") {
    it("should deserialize the string correctly (1)") {
      Given("a default Jackson Scala object mapper")
      initializeDefaultJacksonScalaObjectMapper()

      When("deserializing a Json with all fields available")
      val json: String =
        """
          |{
          | "name":"Tom",
          | "age":"10",
          | "height":175,
          | "weight":65,
          | "occupation":"Engineer"
          |}
          |""".stripMargin
      val output: Person = objectMapper.deserialize[Person](json, classOf[Person])

      Then("the field values should be correct")
      assert(output.name === "Tom")
      assert(output.age === 10)
      assert(output.height === 175)
      assert(output.weight === 65)
    }

    it("should deserialize the string correctly (2)") {
      Given("a default Jackson Scala object mapper")
      initializeDefaultJacksonScalaObjectMapper()

      When("deserializing a Json with no fields available")
      val json: String =
        """
          |{
          | "occupation":"Engineer"
          |}
          |""".stripMargin
      val output: Person = objectMapper.deserialize[Person](json, classOf[Person])

      Then("the default field values should be returned")
      assert(output.name === "NA")
      assert(output.age === 0)
      assert(output.height === 0)
      assert(output.weight === 0)
    }
  }

  describe("Customized Jackson Scala Object Mapper") {
    it("should deserialize the string correctly") {
      Given("a customized Jackson Scala object mapper")
      initializeCustomizedJacksonScalaObjectMapper()

      When("deserializing a Json with all fields available and filter = \"false\"")
      val json: String =
        """
          |{
          | "data":{
          |   "name":"Tom",
          |   "age":"10",
          |   "height":175,
          |   "weight":65
          | },
          | "nested":true
          |}
          |""".stripMargin
      val output: NestedPerson = objectMapper.deserialize[NestedPerson](json, classOf[NestedPerson])

      Then("the field values should be correct")
      assert(output.name === "Nested_Tom") // Check util.CustomizedJacksonScalaObjectMapper
      assert(output.age === 10)
      assert(output.height === 175)
      assert(output.weight === 65)
    }
  }
}


case class Person(name: String = "NA", age: Int = 0, height: Double = 0, weight: Double = 0)


case class NestedPerson(
                         @BeanProperty var name: String = "NA",
                         @BeanProperty var age: Int = 0,
                         @BeanProperty var height: Double = 0d,
                         @BeanProperty var weight: Double = 0d
                       ) {

  def this() {
    this("NA", 0, 0d, 0d)
  }

  @JsonProperty("data")
  private def unpackNestedData(data: Map[String, Object]): Unit = {
    this.name = data.getOrElse("name", "NA").toString
    this.age = data.getOrElse("age", 0).toString.toInt
    this.height = data.getOrElse("height", 0d).toString.toDouble
    this.weight = data.getOrElse("weight", 0d).toString.toDouble
  }

  override def toString: String = {
    s"name: $name, age: $age, height: $height, weight: $weight"
  }
}


class CustomizedJacksonScalaObjectMapper extends JacksonScalaObjectMapper {
  override def deserialize[T](json: String, clazz: Class[T]): T = {
    val raw = objectMapper.readTree(json)
    val output = objectMapper.readValue(json, clazz)

    // Update name if it is from nested data
    if (null != raw.get("nested") || raw.get("nested").asBoolean()) {
      output.getClass.getDeclaredFields
        .foreach {
          f =>
            f.setAccessible(true)
            val res = f.get(output)
            if (f.getName == "name") {
              f.set(output, s"Nested_$res")
            }
            f.setAccessible(false)
        }
    }

    output
  }
}