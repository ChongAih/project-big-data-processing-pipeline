/* project-big-data-processing-pipeline
 *
 * Created: 28/12/21 7:00 pm
 *
 * Description:
 */
package util

import com.typesafe.config.{Config, ConfigFactory}

import scala.reflect.runtime.universe._

object ConfigReader {

  val fallbackConfigResourcePath: String = Const.FALLBACK_CONFIG_RESOURCE_PATH
  val defaultConfig: Config = {
    ConfigFactory.parseResources(fallbackConfigResourcePath)
  }

  def readConfig(configResourcePath: String): Config = {
    val config: Config = {
      ConfigFactory.parseResources(configResourcePath)
        .withFallback(defaultConfig)
        .resolve()
    }
    config
  }

  def getConfigField[T: TypeTag](config: Config, path: String): T = {
    if (config.hasPath(path)) {
      typeOf[T] match {
        case t if t =:= typeOf[String] => config.getString(path).asInstanceOf[T]
        case t if t =:= typeOf[Int] => config.getInt(path).asInstanceOf[T]
        case t if t =:= typeOf[Long] => config.getLong(path).asInstanceOf[T]
        case t if t =:= typeOf[Double] => config.getDouble(path).asInstanceOf[T]
        case t if t =:= typeOf[Boolean] => config.getBoolean(path).asInstanceOf[T]
      }
    } else {
      throw new Exception(s"Missing $path in configuration")
    }
  }

}