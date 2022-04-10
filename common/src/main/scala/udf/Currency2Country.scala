/* project-big-data-processing-pipeline
 *
 * Created: 2/1/22 7:13 pm
 *
 * Description:
 */
package udf

import java.util.{HashMap => JHashMap, Map => JMap}

object Currency2Country extends (String => String) with Serializable {
  val currency2CountryMapping: JMap[String, String] = {
    val temp = new JHashMap[String, String]()
    val currencies = List("IDR", "PHP", "MYR", "SGD")
    val countries = List("ID", "PH", "MY", "SG")
    for (i <- currencies.indices) {
      temp.put(currencies(i), countries(i))
    }
    temp
  }

  val name: String = "currency_to_country"

  override def apply(currency: String): String = {
    currency2CountryMapping.getOrDefault(currency, "")
  }
}

