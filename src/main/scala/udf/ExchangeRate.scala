/* project-big-data-processing-pipeline
 *
 * Created: 1/2/22 1:10 pm
 *
 * Description:
 */
package udf

import com.typesafe.config.Config
import hbase.HBaseClientHelper
import util.{CaffeineCache, ConfigReader, Const}

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}
import java.lang.Double

object ExchangeRate extends ((String, String) => Double)
  with CaffeineCache[String, Double] with Serializable {

  val name = "get_exchange_rate"
  val timezone = "Asia/Singapore"
  val columnQualifier = "exchange_rate"
  var config: Option[Config] = None

  def initialize(config: Config, maxCacheSize: Int = Const.CAFFEINE_MAX_CACHE_SIZE,
                 expireAfterWrite: Int = Const.CAFFEINE_EXPIRE_AFTER_WRITE,
                 expireAfterAccess: Int = Const.CAFFEINE_EXPIRE_AFTER_ACCESS,
                 initCacheSize: Int = Const.CAFFEINE_INIT_CACHE_SIZE,
                 enableRecordStat: Boolean = Const.CAFFEINE_ENABLE_RECORD_STAT): Unit = {
    // Will be initialized at each executor
    this.config = Some(config)
    setParameter(maxCacheSize, expireAfterWrite, expireAfterAccess, initCacheSize, enableRecordStat)
  }

  override def apply(country: String, date: String): Double = {
    val exchangeRate: Double = {
      try {
        // When running in docker container, HBase connection is established.
        // Firstly check the cache data, if not found proceed to retrieve exchange rate from HBase
        // If the data is not found in HBase also, check if the yesterday data does exist
        // If all failed, default value of 0.0d is returned to prevent exception exit
        // Check current date data
        var temp = getExchangeRate(country, date)
        // If no current date data, check for yesterday
        if (temp == null) {
          val newDate = computeDate(date, -1)
          temp = getExchangeRate(country, newDate)
        }
        // If no data, return 0.0d
        if (temp == null) {
          logger.error(s"Exchange rate for region $country dated $date not found, set to default value of 0.0d")
          temp = 0.0d
        }
        temp
      } catch {
        // When running in local IDE for unittest (TxnTest.scala)/ behaviour test (StreamingRunnerTest)
        // no HBase connection is established (due to the need of updating hosts /etc/hosts file),
        // and thus error will be thrown from try loop.
        // Default value of 0.0d is returned to prevent exception exit
        case e: Throwable => {
          logger.error(s"Error occurs when getting exchange rate for region $country dated $date - ${e.toString}")
          logger.error(s"Exchange rate for region $country dated $date is set to default value of 0.0d")
          0.0d
        }
      }
    }
    exchangeRate
  }

  def computeDate(date: String, day: Int): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    sdf.setTimeZone(TimeZone.getTimeZone(timezone))
    val dt: Date = sdf.parse(date)
    // Get new data after computation
    val calendar = Calendar.getInstance()
    calendar.setTime(dt)
    calendar.add(Calendar.DATE, day)
    val newData = calendar.getTime
    sdf.format(newData)
  }

  def getExchangeRate(country: String, date: String): Double = {
    val rowkey = country + date
    // Read through caching
    val cacheData: Option[Double] = getCacheKeyData(rowkey)
    val exchangeRate = cacheData match {
      case Some(x) =>
        // Return if found from cache
        logger.info(s"Getting exchange rate from cache for rowkey: $rowkey")
        x
      case _ =>
        // Read from hbase and save in cache
        logger.info(s"Getting exchange rate from hbase for rowkey: $rowkey")
        val hbaseData = readFromHBase(rowkey)
        if (hbaseData != null) {
          logger.info(s"Caching exchange rate for rowkey: $rowkey")
          setCacheKeyData(rowkey, hbaseData)
        }
        hbaseData
    }
    exchangeRate
  }

  def readFromHBase(rowkey: String): Double = {
    val hbaseZooKeeperQuorum: String = ConfigReader.getConfigField[String](config.get, "hbase.quorum")
    val hbaseZooKeeperPort: String = ConfigReader.getConfigField[String](config.get, "hbase.port")
    val hbaseZNodeParent: String = ConfigReader.getConfigField[String](config.get, "hbase.znodeparent")
    val hbaseClientHelper = new HBaseClientHelper(hbaseZooKeeperQuorum,
      hbaseZooKeeperPort, hbaseZNodeParent)
    val hbaseTable = ConfigReader.getConfigField[String](config.get, "hbase.exrtable")
    val columnFamily = try {
      ConfigReader.getConfigField[String](config.get, "hbase.columnfamily")
    } catch {
      case _: Throwable => "cf"
    }
    val exchangeRate = hbaseClientHelper.get(hbaseTable, rowkey, columnFamily, columnQualifier, classOf[Double])
    hbaseClientHelper.closeConn()
    exchangeRate
  }

}