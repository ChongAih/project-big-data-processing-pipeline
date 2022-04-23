/* project-big-data-processing-pipeline
 *
 * Created: 20/3/22 12:55 pm
 *
 * Description:
 */
package streaming.flink.job

import com.typesafe.config.Config
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.{createTypeInformation, DataStream, OutputTag}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.Table
import org.apache.flink.util.Collector
import streaming.{KafkaInputData, KafkaOutputData, Order, TxnOutput, Value}
import streaming.flink.processor.FlinkKafkaConsumerMessageProcessor
import udf.{FlinkCurrency2Country, FlinkExchangeRate, FlinkSecond2MilliSecond}
import util.ConversionHelper.getMilliSecond


class FlinkTxn extends FlinkJob {
  val order_table: String = "order_tab"

  override def registerUDF(bpTableEnv: StreamTableEnvironment, config: Config): Unit = {
    bpTableEnv.createTemporarySystemFunction(FlinkCurrency2Country.name, classOf[FlinkCurrency2Country])
    bpTableEnv.createTemporarySystemFunction(FlinkSecond2MilliSecond.name, classOf[FlinkSecond2MilliSecond])
    bpTableEnv.createTemporarySystemFunction(FlinkExchangeRate.name, new FlinkExchangeRate(config))
  }

  override def createInputTempView(bpTableEnv: StreamTableEnvironment, kafkaStream: DataStream[KafkaInputData]): Unit = {
    val orderOutputTag = OutputTag[Order](order_table)
    val processedStream = kafkaStream.process(new TxnProcess(orderOutputTag))

    // Get output based on the output tag
    val orderStream = getWatermarkedTimestampedOutput[Order](
      processedStream.getSideOutput(orderOutputTag), // get side output from the processed stream
      "create_time",
      60L
    )

    // Create temporary view based on the output
    bpTableEnv.createTemporaryView(order_table, orderStream, getTempViewFields[Order]: _*)
  }

  override def getSqlQuery: String = {
    s"""
       |WITH orders AS (
       |  SELECT
       |    gmv,
       |    order_id,
       |    user_id,
       |    ${FlinkCurrency2Country.name}(currency) AS country,
       |    ${FlinkSecond2MilliSecond.name}(COALESCE(create_time, 0)) AS ${getEventTimeName},
       |    ${FlinkSecond2MilliSecond.name}(UNIX_TIMESTAMP()) AS processing_time
       |  FROM $order_table
       |  WHERE order_status = 'paid'
       |)
       |SELECT
       |  *,
       |  gmv * ${FlinkExchangeRate.name}(country, CAST(FROM_UNIXTIME(UNIX_TIMESTAMP(), 'yyyy-MM-dd') AS STRING)) AS gmv_usd
       |FROM orders
       |""".stripMargin
  }

  override def getDuplicationFieldNames: List[String] = List("country", "order_id")

  override def getOutputDataStream(bpTableEnv: StreamTableEnvironment,
                                   outputTable: Table): DataStream[KafkaOutputData] = {
    val outputDataStream: DataStream[TxnOutput] = bpTableEnv.toDataStream(outputTable, classOf[TxnOutput])
    outputDataStream.map(data => {
      KafkaOutputData(
        getMilliSecond(data.event_time),
        objectMapper.serialize(data),
        getRowkey[TxnOutput](data, getDuplicationFieldNames)
      )
    })
  }
}


class TxnProcess(orderOutputTag: OutputTag[Order]) extends FlinkKafkaConsumerMessageProcessor[Order] {
  override def processKafkaConsumerMessage(data: KafkaInputData,
                                           context: ProcessFunction[KafkaInputData, Order]#Context,
                                           collector: Collector[Order]): Unit = {
    val valueInput = objectMapper.deserialize[Value](data.kafkaValue, classOf[Value])
    if (valueInput.event.isDefined && valueInput.event.get.database == "order") {
      // Tag each output in a multiple-union-stream process
      val output = objectMapper.deserialize[Order](data.kafkaValue, classOf[Order])
      context.output(orderOutputTag, output)
      // collector.collect(output)
    }
  }
}
