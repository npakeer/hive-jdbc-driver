package veil.hdp.hive.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class TestKerberosKeytab extends BaseConnectionTest {

        /*
        -Djava.security.krb5.conf=C:\ProgramData\MIT\Kerberos5\krb5.ini

        must have JCE installed in proper directory <java_home>/jre/lib/security

     */

    @Override
    public Connection createConnection(String host) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", "hive");

        String url = "jdbc:hive2://" + host + ":10500/jdbc_test?authMode=KERBEROS&krb5ServerPrincipal=hive/hdp2.lab.local@LAB.LOCAL&krb5LocalPrincipal=timve@LAB.LOCAL&krb5LocalKeytab=C:/temp/timve.keytab";

        return new BinaryHiveDriver().connect(url, properties);
    }

}