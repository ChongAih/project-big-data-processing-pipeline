/* project-big-data-processing-pipeline
 *
 * Created: 21/4/22 2:33 pm
 *
 * Description:
 */
package streaming.flink

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.typesafe.config.Config
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{createTypeInformation, CloseableIterator, DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.{EnvironmentSettings, Table}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import streaming.flink.job.FlinkJob
import streaming.{KafkaInputData, KafkaOutputData}
import util.ConfigReader

import scala.reflect.ClassTag

class FlinkUnitTestRunner {

  val senv: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
  val settings: EnvironmentSettings = EnvironmentSettings
    .newInstance()
    .inStreamingMode()
    .build()
  val tenv: StreamTableEnvironment = StreamTableEnvironment.create(senv, settings)
  var job: Option[FlinkJob] = None

  def createJob(jobName: String): Unit = {
    job = Some({
      Class.forName(s"streaming.flink.job.$jobName")
        .getConstructor()
        .newInstance()
        .asInstanceOf[FlinkJob]
    })
  }

  def registerUDF(configResourcePath: String): Unit = {
    val config: Config = ConfigReader.readConfig(s"config/$configResourcePath")
    job.get.registerUDF(tenv, config)
  }

  def processJson[T: TypeInformation](filter: Boolean, jsons: String*)(implicit ct: ClassTag[T]): CloseableIterator[T] = {
    // Create data stream from list of JSONs
    val jsonCollection = jsons.map(j => {
      KafkaInputData(j, 0)
    })
    val inputDataStream = senv.fromCollection(jsonCollection)

    // Create temporary view & process data
    job.get.createInputTempView(tenv, inputDataStream)

    val sqlTable: Table = tenv.sqlQuery(job.get.getSqlQuery)
    val outputDataStream = {
      val sqlDataStream: DataStream[KafkaOutputData] = job.get.getOutputDataStream(tenv, sqlTable)
      if (filter) {
        job.get.deduplicationAndTimeFilter(sqlDataStream)
      } else {
        sqlDataStream
      }
    }

    // Collect output
    val resultTable = outputDataStream.map(x => {
      val objectMapper = new ObjectMapper() with ScalaObjectMapper
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.readValue(x.kafkaValue, ct.runtimeClass.asInstanceOf[Class[T]])
    })

    resultTable.executeAndCollect()
  }

}