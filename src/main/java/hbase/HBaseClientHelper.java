/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 5:29 pm
 *
 * Description:
 */
package hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class HBaseClientHelper implements Closeable {
    private transient final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final Connection conn;
    private Table table;

    public HBaseClientHelper() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + "2181");
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        conn = ConnectionFactory.createConnection(conf);
    }

    public HBaseClientHelper(String zookeeperQuorum,
                             String zookeeperPort,
                             String znodeParent) throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", zookeeperQuorum);
        conf.set("hbase.zookeeper.property.clientPort", "" + zookeeperPort);
        conf.set("zookeeper.znode.parent", znodeParent);
        conn = ConnectionFactory.createConnection(conf);
    }

    public void getTable(String hbaseTable) throws IOException {
        table = conn.getTable(TableName.valueOf(hbaseTable));
    }

    public void closeTable() throws IOException {
        table.close();
    }

    public void closeConn() throws IOException {
        conn.close();
    }

    public <T> Map<String, T> scan(String hbaseTable, String columnFamily,
                                   String columnQualifier, Class<T> clazz) throws IOException {
        // Initiate connection to HBase table
        getTable(hbaseTable);
        // Scan targeted columnFamily/columnQualifier
        Scan scan = new Scan();
        scan.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier));
        ResultScanner scanner = table.getScanner(scan);
        Map<String, T> output = new HashMap<>();
        for (Result result : scanner) {
            T value = castByteValueToType(
                    result.getValue(Bytes.toBytes(columnFamily),
                            Bytes.toBytes(columnQualifier)),
                    clazz);
            String key = Bytes.toString(result.getRow());
            output.put(key, value);
        }
        scanner.close();
        return output;
    }

    public <T> T get(String hbaseTable, String rowkey,
                     String columnFamily, String columnQualifier,
                     Class<T> clazz) throws IOException {
        // Initiate connection to HBase table
        getTable(hbaseTable);
        // Read value from HBase table based on the rowkey and convert to targeted type
        Get get = new Get(Bytes.toBytes(rowkey));
        Result result = this.table.get(get);
        T resultValue = castByteValueToType(
                result.getValue(Bytes.toBytes(columnFamily),
                        Bytes.toBytes(columnQualifier)),
                clazz);
        logger.info(String.format(
                "HBase response -> Read value from HBase, table: %s, rowkey: %s, column: %s:%s, value: %s",
                hbaseTable, rowkey, columnFamily, columnQualifier, String.valueOf(resultValue)));
        // Close table connection and return value
        closeTable();
        return resultValue;
    }

    public void put(String hbaseTable, String rowkey,
                    String columnFamily, String columnQualifier,
                    Object value) throws IOException {
        // Initiate connection to HBase table
        getTable(hbaseTable);
        // Read value from HBase table based on the rowkey and convert to targeted type
        Put put = new Put(Bytes.toBytes(rowkey));
        put.addColumn(Bytes.toBytes(columnFamily),
                Bytes.toBytes(columnQualifier),
                castObjectWriteValueToByte(value));
        table.put(put);
        logger.info(String.format(
                "HBase response -> table: %s, rowkey: %s, column: %s:%s value: %s added",
                hbaseTable, rowkey, columnFamily, columnQualifier, value));
        // Close table connection and return value
        closeTable();
    }

    public void delete(String hbaseTable, String rowkey) throws IOException {
        getTable(hbaseTable);
        Delete delete = new Delete(Bytes.toBytes(rowkey));
        table.delete(delete);
        logger.info(String.format(
                "HBase response -> table: %s, rowkey: %s deleted", hbaseTable, rowkey));
        closeTable();
    }

    public void delete(String hbaseTable, String rowkey,
                       String columnFamily, String columnQualifier) throws IOException {
        getTable(hbaseTable);
        Delete delete = new Delete(Bytes.toBytes(rowkey));
        delete.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier));
        table.delete(delete);
        logger.info(String.format(
                "HBase response -> table: %s, rowkey: %s, column: %s:%s deleted",
                hbaseTable, rowkey, columnFamily, columnQualifier));
        closeTable();
    }

    public boolean checkAndPut(String hbaseTable, String rowkey,
                            String columnFamily, String columnQualifier,
                            Object checkedValue, Object value) throws IOException {
        // Initiate connection to HBase table
        getTable(hbaseTable);
        // Read value from HBase table based on the rowkey and convert to targeted type
        Put put = new Put(Bytes.toBytes(rowkey));
        put.addColumn(Bytes.toBytes(columnFamily),
                Bytes.toBytes(columnQualifier),
                castObjectWriteValueToByte(value));
        // Get byte value of checkedValue
        byte[] byteCheckedValue;
        if (checkedValue == null) {
            byteCheckedValue = null;
        } else {
            byteCheckedValue = castObjectWriteValueToByte(checkedValue);
        }
        // Execute checkAndPut
        boolean checkPutStatus = table.checkAndPut(Bytes.toBytes(rowkey), Bytes.toBytes(columnFamily),
                Bytes.toBytes(columnQualifier), byteCheckedValue, put);
        logger.info(String.format("HBase response -> checkPutStatus: %s", checkPutStatus));
        // Close table connection and return value
        closeTable();
        return checkPutStatus;
    }

    public boolean checkAndDelete(String hbaseTable, String rowkey,
                               String columnFamily, String columnQualifier,
                               Object checkedValue) throws IOException {
        getTable(hbaseTable);
        Delete deleteCD = new Delete(Bytes.toBytes(rowkey));
        deleteCD.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier));
        // Get byte value of checkedValue
        byte[] byteCheckedValue;
        if (checkedValue == null) {
            byteCheckedValue = null;
        } else {
            byteCheckedValue = castObjectWriteValueToByte(checkedValue);
        }
        boolean checkDeleteStatus = table.checkAndDelete(Bytes.toBytes(rowkey), Bytes.toBytes(columnFamily),
                Bytes.toBytes(columnQualifier), byteCheckedValue, deleteCD);
        logger.info(String.format("HBase response -> checkDeleteStatus: %s", checkDeleteStatus));
        closeTable();
        return checkDeleteStatus;
    }

    public byte[] castObjectWriteValueToByte(Object writeValue) {
        String className = writeValue.getClass().getSimpleName().toLowerCase();
        String writeValueString = writeValue.toString();
        logger.info(String.format("HBase response -> The classname of writeValue: %s", className));
        switch (className) {
            case "short":
                return Bytes.toBytes(new Short(writeValueString));
            case "int":
                return Bytes.toBytes(new Integer(writeValueString));
            case "long":
                return Bytes.toBytes(new Long(writeValueString));
            case "float":
                return Bytes.toBytes(new Float(writeValueString));
            case "double":
                return Bytes.toBytes(new Double(writeValueString));
            case "bigdecimal":
                return Bytes.toBytes(new BigDecimal(writeValueString));
            case "string":
                return Bytes.toBytes(writeValueString);
            case "boolean":
                return Bytes.toBytes(Boolean.parseBoolean(writeValueString));
            default:
                logger.info(String.format("No matching class found for %s, return null", writeValue));
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T castByteValueToType(byte[] byteValue, Class<T> clazz) {
        String typeName = clazz.getSimpleName().toLowerCase();
        logger.info(String.format("HBase response -> The classname of targeted cast type: %s", typeName));
        switch (typeName) {
            case "short":
                return (T) new Short(Bytes.toShort(byteValue));
            case "int":
                return (T) new Integer(Bytes.toInt(byteValue));
            case "long":
                return (T) new Long(Bytes.toLong(byteValue));
            case "float":
                return (T) new Float(Bytes.toFloat(byteValue));
            case "double":
                return (T) new Double(Bytes.toDouble(byteValue));
            case "bigdecimal":
                return (T) Bytes.toBigDecimal(byteValue);
            case "string":
                return (T) Bytes.toString(byteValue);
            case "boolean":
                return (T) Boolean.valueOf(Bytes.toBoolean(byteValue));
            default:
                return null;
        }
    }

    @Override
    public void close() throws IOException {
        closeTable();
        closeConn();
    }
}