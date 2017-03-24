package veil.hdp.hive.jdbc;

import org.apache.hive.service.cli.RowSet;
import org.apache.hive.service.cli.RowSetFactory;
import org.apache.hive.service.cli.thrift.TFetchOrientation;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.apache.hive.service.cli.thrift.TRowSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HiveResultSet extends AbstractResultSet {

    private static final Logger log = LoggerFactory.getLogger(HiveResultSet.class);

    // constructor
    private final HiveConnection connection;
    private final HiveStatement statement;
    private final TOperationHandle statementHandle;
    private final Schema schema;

    // private
    private RowSet rowSet;
    private Iterator<Object[]> rowSetIterator;
    private Object[] row;

    // public getter & setter
    private int fetchSize;
    private int fetchDirection;
    private int resultSetType;
    private int resultSetConcurrency;
    private int resultSetHoldability;
    private SQLWarning sqlWarning = null;

    // public getter only
    private int rowCount;
    private final AtomicBoolean closed = new AtomicBoolean(true);
    private boolean lastColumnNull;


    HiveResultSet(HiveStatement statement, TOperationHandle statementHandle, Schema schema) throws SQLException {
        this.connection = (HiveConnection)statement.getConnection();
        this.statement = statement;
        this.schema = schema;
        this.statementHandle = statementHandle;
        this.fetchDirection = statement.getFetchDirection();
        this.fetchSize = statement.getFetchSize();
        this.resultSetType = statement.getResultSetType();
        this.resultSetConcurrency = statement.getResultSetConcurrency();
        this.resultSetHoldability = statement.getResultSetHoldability();
    }

    @Override
    public boolean next() throws SQLException {

        // if a ResultSet is manually created then statementHandle is always null;
        if (statementHandle == null) {
            return false;
        }

        if (statement.getMaxRows() > 0 && rowCount >= statement.getMaxRows()) {
            return false;
        }


        if (rowSet == null || !rowSetIterator.hasNext()) {
            TRowSet results = HiveServiceUtils.fetchResults(connection.getClient(), statementHandle, TFetchOrientation.FETCH_NEXT, fetchSize);
            rowSet = RowSetFactory.create(results, connection.getProtocolVersion());
            rowSetIterator = rowSet.iterator();
        }

        if (rowSetIterator.hasNext()) {
            row = rowSetIterator.next();
        } else {
            return false;
        }

        rowCount++;

        return true;
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() throws SQLException {
        if (closed.compareAndSet(false, true)) {

            if (log.isDebugEnabled()) {
                log.debug("attempting to close {}", this.getClass().getName());
            }

            rowSet = null;
            rowSetIterator = null;
            row = null;
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.fetchSize = rows;
    }

    @Override
    public int getType() throws SQLException {
        return resultSetType;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return rowCount == 0;
    }

    @Override
    public int getRow() throws SQLException {
        return rowCount;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return ResultSetUtils.findColumnIndex(schema, columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return (BigDecimal) getColumnValue(columnIndex, HiveType.DECIMAL);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(findColumn(columnLabel));
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return (Boolean) getColumnValue(columnIndex, HiveType.BOOLEAN);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    @Override
    public int getConcurrency() throws SQLException {
        return resultSetConcurrency;
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        return (Date) getColumnValue(columnIndex, HiveType.DATE);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return (Double) getColumnValue(columnIndex, HiveType.DOUBLE);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(findColumn(columnLabel));
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return fetchDirection;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        this.fetchDirection = direction;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return (Float) getColumnValue(columnIndex, HiveType.FLOAT);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(findColumn(columnLabel));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return (Integer) getColumnValue(columnIndex, HiveType.INTEGER);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(findColumn(columnLabel));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return (Long) getColumnValue(columnIndex, HiveType.BIG_INT);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(findColumn(columnLabel));
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getColumnValue(columnIndex, null);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return (Short) getColumnValue(columnIndex, HiveType.SMALL_INT);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(findColumn(columnLabel));
    }

    @Override
    public Statement getStatement() throws SQLException {
        return this.statement;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return (String) getColumnValue(columnIndex, HiveType.STRING);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return (Timestamp) getColumnValue(columnIndex, HiveType.TIMESTAMP);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }


    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new HiveResultSetMetaData(schema);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return (Byte) getColumnValue(columnIndex, HiveType.TINY_INT);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(findColumn(columnLabel));
    }

    @Override
    public int getHoldability() throws SQLException {
        return resultSetHoldability;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return lastColumnNull;
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return (byte[]) getColumnValue(columnIndex, HiveType.BINARY);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return getValueAsStream(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return getBinaryStream(findColumn(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return sqlWarning;
    }

    @Override
    public void clearWarnings() throws SQLException {
        sqlWarning = null;
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        return getValueAsTime(columnIndex);
    }


    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }


    // --------------------- TODO --------------------------------------------------------------------------------------------------------------------------------------



    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return super.getAsciiStream(columnIndex);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return super.getAsciiStream(columnLabel);
    }


    @Override
    public String getCursorName() throws SQLException {
        return super.getCursorName();
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return super.getCharacterStream(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return super.getCharacterStream(columnLabel);
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return super.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return super.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return super.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        super.beforeFirst();
    }

    @Override
    public void afterLast() throws SQLException {
        super.afterLast();
    }

    @Override
    public boolean first() throws SQLException {
        return super.first();
    }

    @Override
    public boolean last() throws SQLException {
        return super.last();
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return super.absolute(row);
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return super.relative(rows);
    }

    @Override
    public boolean previous() throws SQLException {
        return super.previous();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return super.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return super.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return super.rowDeleted();
    }

    @Override
    public void insertRow() throws SQLException {
        super.insertRow();
    }

    @Override
    public void deleteRow() throws SQLException {
        super.deleteRow();
    }

    @Override
    public void refreshRow() throws SQLException {
        super.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        super.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        super.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        super.moveToCurrentRow();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return super.getObject(columnIndex, map);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        return super.getRef(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        return super.getBlob(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        return super.getClob(columnIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return super.getArray(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return super.getObject(columnLabel, map);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return super.getRef(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return super.getBlob(columnLabel);
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return super.getClob(columnLabel);
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return super.getArray(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return super.getDate(columnIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return super.getDate(columnLabel, cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return super.getTime(columnIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return super.getTime(columnLabel, cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return super.getTimestamp(columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return super.getTimestamp(columnLabel, cal);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return super.getURL(columnIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return super.getURL(columnLabel);
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return super.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return super.getRowId(columnLabel);
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return super.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return super.getNClob(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return super.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return super.getSQLXML(columnLabel);
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return super.getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return super.getNString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return super.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return super.getNCharacterStream(columnLabel);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return super.getObject(columnIndex, type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return super.getObject(columnLabel, type);
    }


    // --------------------- TODO --------------------------------------------------------------------------------------------------------------------------------------

    private Object getColumnValue(int columnIndex, HiveType targetType) throws SQLException {

        Object columnValue = ResultSetUtils.getColumnValue(schema, row, columnIndex, targetType);

        lastColumnNull = columnValue == null;

        return columnValue;
    }

    private InputStream getValueAsStream(int columnIndex) throws SQLException {

        InputStream columnValue = ResultSetUtils.getColumnValueAsStream(schema, row, columnIndex);

        lastColumnNull = columnValue == null;

        return columnValue;
    }

    private Time getValueAsTime(int columnIndex) throws SQLException {

        Time columnValue = ResultSetUtils.getColumnValueAsTime(schema, row, columnIndex);

        lastColumnNull = columnValue == null;

        return columnValue;
    }


}
