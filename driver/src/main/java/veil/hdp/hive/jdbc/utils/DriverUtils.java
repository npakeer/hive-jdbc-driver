/*
 *    Copyright 2018 Timothy J Veil
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package veil.hdp.hive.jdbc.utils;


import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import veil.hdp.hive.jdbc.HiveDriverProperty;
import veil.hdp.hive.jdbc.HiveSQLException;

import java.net.URI;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DriverUtils {

    private static final Logger log = LogManager.getLogger(DriverUtils.class);

    private static final String JDBC_PART = "jdbc:";
    private static final String HIVE2_PART = "hive2:";
    private static final String JDBC_HIVE2_PREFIX = JDBC_PART + HIVE2_PART + "//";
    private static final String FORWARD_SLASH = "/";
    private static final Pattern FORWARD_SLASH_PATTERN = Pattern.compile(FORWARD_SLASH);
    private static final Pattern JDBC_PATTERN = Pattern.compile(JDBC_PART, Pattern.LITERAL);
    private static final String EMPTY_STRING = "";

    private DriverUtils() {
    }

    public static boolean acceptURL(String url) throws SQLException {

        if (url == null) {
            throw new HiveSQLException("url is null");
        }

        return url.startsWith(JDBC_HIVE2_PREFIX);
    }

    public static String buildUrl(Properties properties) {
        return JDBC_HIVE2_PREFIX + HiveDriverProperty.HOST_NAME.get(properties) + ':' + HiveDriverProperty.PORT_NUMBER.getInt(properties) + '/' + HiveDriverProperty.DATABASE_NAME.get(properties);
    }


    public static Properties buildProperties(String url, Properties suppliedProperties) {

        Properties properties = new Properties();

        // load defaults
        loadDefaultProperties(properties);

        // loads properties supplied by the JDBC api Driver.connect method
        loadSuppliedProperties(suppliedProperties, properties);

        // parse the url supplied by the JDBC api Driver.connect method
        parseUrl(url, properties);

        return properties;

    }


    private static String normalizeKey(String key) {

        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        HiveDriverProperty property = HiveDriverProperty.forKeyIgnoreCase(key);

        if (property != null) {
            return property.getKey();
        }

        return key;
    }


    private static void loadSuppliedProperties(Properties suppliedProperties, Properties properties) {
        for (String key : suppliedProperties.stringPropertyNames()) {

            String value = StringUtils.trimToNull(suppliedProperties.getProperty(key));

            if (value != null) {
                properties.setProperty(normalizeKey(key), value);
            }

        }
    }

    private static void loadDefaultProperties(Properties properties) {
        for (HiveDriverProperty property : HiveDriverProperty.values()) {
            property.setDefaultValue(properties);
        }
    }

    public static DriverPropertyInfo[] buildDriverPropertyInfo(String url, Properties suppliedProperties) {
        Properties properties = buildProperties(url, suppliedProperties);

        HiveDriverProperty[] driverProperties = HiveDriverProperty.values();

        DriverPropertyInfo[] driverPropertyInfoArray = new DriverPropertyInfo[driverProperties.length];

        for (int i = 0; i < driverPropertyInfoArray.length; i++) {
            driverPropertyInfoArray[i] = driverProperties[i].toDriverPropertyInfo(properties);
        }

        return driverPropertyInfoArray;

    }


    private static void validateProperties(Properties properties) {

        for (String key : properties.stringPropertyNames()) {

            boolean valid = false;

            if (HiveDriverProperty.forKeyIgnoreCase(key) != null) {
                valid = true;
            }

            if (key.startsWith("hive.")) {
                valid = true;
            }

            if (!valid) {
                log.warn("property [{}] is unknown and possibly invalid.  This could be a configuration issue (ie. wrong case, misspelling, etc.) or a custom property.  It is wise to double check this key/value pair", key);
            }

        }

    }


    private static void parseUrl(String url, Properties properties) {

        URI uri = URI.create(stripPrefix(url));

        String databaseName = StringUtils.trimToNull(getDatabaseName(uri));

        HiveDriverProperty.DATABASE_NAME.set(properties, databaseName);

        String uriQuery = uri.getQuery();

        parseQueryParameters(uriQuery, properties);

        boolean zookeeperDiscoveryEnabled = HiveDriverProperty.ZOOKEEPER_DISCOVERY_ENABLED.getBoolean(properties);

        if (zookeeperDiscoveryEnabled) {

            String authority = uri.getAuthority();

            ZookeeperUtils.loadPropertiesFromZookeeper(authority, properties);

        } else {

            HiveDriverProperty.HOST_NAME.set(properties, uri.getHost());

            if (uri.getPort() != -1) {
                HiveDriverProperty.PORT_NUMBER.set(properties, uri.getPort());
            }
        }

        validateProperties(properties);

    }

    private static void parseQueryParameters(String uriQuery, Properties properties) {

        Map<String, String> parameters = new HashMap<>();

        if (uriQuery != null) {
            parameters.putAll(Splitter.on("&").trimResults().omitEmptyStrings().withKeyValueSeparator("=").split(uriQuery));
        }

        for (Entry<String, String> entry : parameters.entrySet()) {
            String value = StringUtils.trimToNull(entry.getValue());

            if (value != null) {
                properties.setProperty(normalizeKey(entry.getKey()), value);
            }
        }

    }

    private static String getDatabaseName(URI uri) {
        String path = uri.getPath();

        if (path != null && path.startsWith(FORWARD_SLASH)) {
            return FORWARD_SLASH_PATTERN.matcher(path).replaceFirst(EMPTY_STRING);
        }

        return null;
    }


    private static String stripPrefix(String url) {
        return JDBC_PATTERN.matcher(url).replaceAll(Matcher.quoteReplacement(EMPTY_STRING));

    }

    public static void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }


}
