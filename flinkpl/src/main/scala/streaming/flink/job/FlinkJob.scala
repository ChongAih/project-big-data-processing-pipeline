/* project-big-data-processing-pipeline
 *
 * Created: 19/3/22 7:29 pm
 *
 * Description:
 */
package streaming.flink.job

import com.typesafe.config.Config
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.functions.FilterFunction
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{createTypeInformation, DataStream}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.{ApiExpression, Expressions, Table}
import streaming.{KafkaInputData, KafkaOutputData}
import streaming.flink.processor.FlinkDeduplicationFilter
import util.ConversionHelper.getMilliSecond
import util.JacksonScalaObjectMapper
import util.TimeFilter.checkWithinProcessingWindow

import java.time.Duration
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

abstract class FlinkJob extends Serializable {
  val objectMapper = new JacksonScalaObjectMapper()

  def registerUDF(bpTableEnv: StreamTableEnvironment, config: Config): Unit

  def createInputTempView(bpTableEnv: StreamTableEnvironment, kafkaStream: DataStream[KafkaInputData]): Unit

  def getSqlQuery: String

  def getDuplicationFieldNames: List[String]

  def getOutputDataStream(bpTableEnv: StreamTableEnvironment, outputTable: Table): DataStream[KafkaOutputData]

  final def getEventTimeName: String = "event_time"

  final def getTempViewFields[T: TypeTag]: List[ApiExpression] = {
    val annotations = symbolOf[T].asClass.primaryConstructor.typeSignature.paramLists.head.map(s => s -> s.annotations)
    var eventTimeFieldName: String = ""
    val ApiExpressionList = annotations.map(symbolAndAnnotation => {
      if (symbolAndAnnotation._2.nonEmpty) {
        eventTimeFieldName = symbolAndAnnotation._1.name.toString
      }
      Expressions.$(symbolAndAnnotation._1.name.toString)
    })
    if (eventTimeFieldName.nonEmpty) {
      ApiExpressionList ++ List(Expressions.$("proctime").proctime(), Expressions.$(eventTimeFieldName + "_rowtime").rowtime())
    }
    else {
      ApiExpressionList ++ List(Expressions.$("proctime").proctime())
    }
  }

  final def getWatermarkedTimestampedOutput[T: TypeInformation](ds: DataStream[T],
                                                                eventTimeFieldName: String,
                                                                watermarkDuration: Long): DataStream[T] = {
    ds.assignTimestampsAndWatermarks(
      WatermarkStrategy.forBoundedOutOfOrderness[T](Duration.ofMinutes(watermarkDuration))
        .withTimestampAssigner(
          new SerializableTimestampAssigner[T] {
            override def extractTimestamp(element: T, recordTimestamp: Long): Long = {
              // Use the value of the stated field as event time
              val eventTime = element.getClass.getDeclaredFields
                .filter(f => if (eventTimeFieldName == f.getName) true else false)
                .map(f => {
                  f.setAccessible(true)
                  val res = f.get(element)
                  f.setAccessible(false)
                  res
                }).mkString("").toLong
              getMilliSecond(eventTime)
            }
          }
        )
    )
  }

  final def getRowkey[T: ClassTag](u: T, fieldNames: List[String] = getDuplicationFieldNames): String = {
    u.getClass
      .getDeclaredFields
      .map {f =>
        f.setAccessible(true)
        val res = f.get(u)
        f.setAccessible(false)
        if (fieldNames.contains(f.getName)) res else ""
      }
      .mkString("")
  }

  final def deduplicationAndTimeFilter(ds: DataStream[KafkaOutputData]): DataStream[KafkaOutputData] = {
    ds.filter(new FilterFunction[KafkaOutputData] {
      override def filter(data: KafkaOutputData): Boolean = {
        checkWithinProcessingWindow(data.eventTime)
      }
    }).keyBy(item => {
      item.rowkey
    }).flatMap(new FlinkDeduplicationFilter)
  }

}