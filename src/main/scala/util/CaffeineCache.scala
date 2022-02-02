/* project-big-data-processing-pipeline
 *
 * Created: 1/2/22 10:27 am
 *
 * Description:
 */
package util

import com.github.benmanes.caffeine.cache.{Cache, Caffeine, Scheduler}
import org.apache.log4j.Logger

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, TimeUnit}

trait CaffeineCache[T1 <: Object, T2 <: Object] {

  var maxCacheSize: Int = Const.CAFFEINE_MAX_CACHE_SIZE
  var expireAfterWrite: Int = Const.CAFFEINE_EXPIRE_AFTER_WRITE
  var expireAfterAccess: Int = Const.CAFFEINE_EXPIRE_AFTER_ACCESS
  var initCacheSize: Int = Const.CAFFEINE_INIT_CACHE_SIZE
  var enableRecordStat: Boolean = Const.CAFFEINE_ENABLE_RECORD_STAT

  // Setting it to be @transient lazy val so that it can be initialized in each executor when it comes to be used
  // lazy val denotes a field that will only be calculated once it is accessed for the first time and is then stored for future reference
  @transient lazy val logger: Logger = LoggerCreator.getLogger(this.getClass.getSimpleName)
  // Synchronized instance affects performance
  // AtomicReference to allow concurrent access but can only update atomically
  @transient lazy val atomicCache: AtomicReference[Cache[T1, T2]] = setUpCache()

  // User can update parameter before atomicCache is used
  def setParameter(maxCacheSize: Int, expireAfterWrite: Int, expireAfterAccess: Int,
                   initCacheSize: Int, enableRecordStat: Boolean): Unit = {
    this.maxCacheSize = maxCacheSize
    this.expireAfterWrite = expireAfterWrite
    this.expireAfterAccess = expireAfterAccess
    this.initCacheSize = initCacheSize
    this.enableRecordStat = enableRecordStat
  }

  def setUpCache(): AtomicReference[Cache[T1, T2]] = {
    val builder = {
      Caffeine.newBuilder()
        .maximumSize(maxCacheSize)
        .initialCapacity(initCacheSize)
    }
    if (expireAfterAccess > 0) {
      // Allow update of old data
      builder.expireAfterAccess(expireAfterAccess, TimeUnit.MINUTES)
    }
    if (expireAfterWrite > 0) {
      // Allow update of old data
      builder.expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)
    }
    if (enableRecordStat) {
      builder.recordStats()
    }
    val cache: Cache[T1, T2] = builder.build[T1, T2]()
    logger.info("Successfully setting up an atomic cache")
    new AtomicReference[Cache[T1, T2]](cache)
  }

  def getCacheKeyData(key: T1): Option[T2] = {
    val currentCache: Cache[T1, T2] = atomicCache.get()
    logger.info(s"Current atomic cache instance: $currentCache")
    logger.info(s"Searching cache data for key: $key")
    if (currentCache.getIfPresent(key) != null) {
      Option(currentCache.getIfPresent(key))
    } else {
      None
    }
  }

  def setCacheKeyData(key: T1, value: T2): Unit = {
    val currentCache: Cache[T1, T2] = atomicCache.get()
    currentCache.put(key, value)
    atomicCache.set(currentCache)
  }

}