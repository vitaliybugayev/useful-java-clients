package client.mybatis;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

/**
 * Minimal MyBatis client that supports both programmatic and XML-based configuration.
 */
public class MyBatisClient implements AutoCloseable {

    private final DataSource dataSource;
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * Programmatic configuration using pooled DataSource.
     */
    public MyBatisClient(String driver, String url, String username, String password) {
        this.dataSource = new PooledDataSource(driver, url, username, password);
        var txFactory = new JdbcTransactionFactory();
        var environment = new Environment("default", txFactory, dataSource);
        var configuration = new Configuration(environment);
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    /**
     * XML configuration loaded from classpath resource (e.g., "mybatis-config.xml").
     * Properties may contain driver, url, username, password placeholders used by the config.
     */
    public MyBatisClient(String xmlConfigResource, Properties properties) {
        try (Reader reader = Resources.getResourceAsReader(xmlConfigResource)) {
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MyBatis config: " + xmlConfigResource, e);
        }
        var env = this.sqlSessionFactory.getConfiguration().getEnvironment();
        this.dataSource = env != null ? env.getDataSource() : null;
    }

    public void registerMapper(Class<?> mapper) {
        var cfg = sqlSessionFactory.getConfiguration();
        if (!cfg.hasMapper(mapper)) {
            cfg.addMapper(mapper);
        }
    }

    public SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    @Override
    public void close() {
        if (dataSource instanceof PooledDataSource pds) {
            pds.forceCloseAll();
        }
    }
}
