package veil.hdp.hive.jdbc.data;

import veil.hdp.hive.jdbc.metadata.ColumnDescriptor;

import java.sql.SQLException;

public class ShortColumn extends BaseColumn<Short> {

    ShortColumn(ColumnDescriptor descriptor, Short value) {
        super(descriptor, value);
    }

    @Override
    public Short getValue() {
        return value != null ? value : 0;
    }

    @Override
    public Short asShort() throws SQLException {
        return getValue();
    }

    @Override
    public String asString() throws SQLException {
        return Short.toString(getValue());
    }

    @Override
    public Boolean asBoolean() throws SQLException {
        if (value != null) {
            return value == 1;
        }

        return null;
    }

    @Override
    public Float asFloat() throws SQLException {
        log.warn("may lose precision going from {} to {}; value [{}]", Short.class, Float.class, value);

        return getValue().floatValue();
    }


    @Override
    public Integer asInt() throws SQLException {
        log.warn("may lose precision going from {} to {}; value [{}]", Short.class, Integer.class, value);

        return getValue().intValue();

    }

    @Override
    public Double asDouble() throws SQLException {
        log.warn("may lose precision going from {} to {}; value [{}]", Short.class, Double.class, value);

        return getValue().doubleValue();
    }

    @Override
    public Long asLong() throws SQLException {
        log.warn("may lose precision going from {} to {}; value [{}]", Short.class, Long.class, value);

        return getValue().longValue();
    }


    @Override
    public Byte asByte() throws SQLException {
        log.warn("may lose precision going from {} to {}; value [{}]", Short.class, Byte.class, value);

        return getValue().byteValue();
    }
}