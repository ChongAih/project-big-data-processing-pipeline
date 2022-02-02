/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 6:41 pm
 *
 * Description:
 */
package util

import com.typesafe.config.Config
import hbase.util.Salting
import hbase.HBaseClientHelper
import org.apache.spark.sql.Row

// Since Spark cannot keep track of job state (except for those in streaming join),
// HBase is deployed to check if a certain data has been written before
class DeduplicationFilter(config: Config, deDuplicationFieldNames: List[String])
  extends ((Row) => Boolean) with Serializable {

  // Setting it to be @transient lazy val so that it can be initialized in each executor when it comes to be used
  // lazy val denotes a field that will only be calculated once it is accessed for the first time and is then stored for future reference
  lazy val hbaseZooKeeperQuorum: String = ConfigReader.getConfigField[String](config, "hbase.quorum")
  lazy val hbaseZooKeeperPort: String = ConfigReader.getConfigField[String](config, "hbase.port")
  lazy val hbaseZNodeParent: String = ConfigReader.getConfigField[String](config, "hbase.znodeparent")
  lazy val hbaseTable: String = ConfigReader.getConfigField[String](config, "hbase.table")
  lazy val columnFamily: String = try {
    ConfigReader.getConfigField[String](config, "hbase.columnfamily")
  } catch {
    case _: Throwable => "cf"
  }
  @transient lazy val hbaseClientHelper: HBaseClientHelper = new HBaseClientHelper(hbaseZooKeeperQuorum,
    hbaseZooKeeperPort, hbaseZNodeParent)

  override def apply(row: Row): Boolean = {
    val saltedKey = {
      val key = deDuplicationFieldNames.map(field => row.getAs(field).toString).mkString
      Salting.getSaltedKey(key)
    }
    val writeToHBase = hbaseClientHelper.checkAndPut(hbaseTable, saltedKey,
      columnFamily, "state", null, 1)
    writeToHBase
  }

}