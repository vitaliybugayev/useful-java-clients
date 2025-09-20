package client.mybatis;

import java.util.Properties;

public class UsageExample {

    public void example() {
        // Programmatic configuration (bring your JDBC driver in the consuming app, e.g., PostgreSQL)
        var driver = "org.postgresql.Driver";
        var url = "jdbc:postgresql://localhost:5432/app";
        var user = "user";
        var pass = "pass";

        try (var client = new MyBatisClient(driver, url, user, pass)) {
            client.registerMapper(UserMapper.class);

            try (var session = client.openSession()) {
                var mapper = session.getMapper(UserMapper.class);
                var userEntity = mapper.selectById(1L);
                System.out.println("User: " + userEntity);
                session.commit();
            }
        }
    }

    public void exampleWithXml() {
        // XML-based configuration (src/main/resources/mybatis-config.xml)
        var props = new Properties();
        props.setProperty("driver", "org.postgresql.Driver");
        props.setProperty("url", "jdbc:postgresql://localhost:5432/app");
        props.setProperty("username", "user");
        props.setProperty("password", "pass");

        try (var client = new MyBatisClient("mybatis-config.xml", props)) {
            try (var session = client.openSession()) {
                var mapper = session.getMapper(UserMapper.class);
                var userEntity = mapper.selectById(1L);
                System.out.println("User: " + userEntity);
                session.commit();
            }
        }
    }
}
