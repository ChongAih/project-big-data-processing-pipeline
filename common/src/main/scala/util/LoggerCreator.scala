/* project-big-data-processing-pipeline
 *
 * Created: 29/12/21 4:32 pm
 *
 * Description:
 */
package util

import org.apache.log4j.Logger

object LoggerCreator {

  def getLogger(loggerName: String): Logger = {
    // Not needed as log34j.properties is loaded automatically
    // PropertyConfigurator.configure(log4jConfigPath)
    Logger.getLogger(loggerName)
  }
}