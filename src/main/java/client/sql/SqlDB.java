package client.sql;

import org.testng.TestException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlDB {

    private Connection connection;
    private final String dbUrl;
    private final String user;
    private final String password;

    public SqlDB(String dbUrl, String user, String password) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(dbUrl, user, password);
            } catch (SQLException e) {
                throw new TestException(e);
            }
        }
        return connection;
    }

    public void closeConnection() {
        if (connection == null) return;
        try {
            connection.close();
            connection = null;
        } catch (SQLException e) {
            throw new TestException(e);
        }
    }
}
