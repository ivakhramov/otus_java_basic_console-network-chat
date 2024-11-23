package ru.ivakhramov.java.basic.chat.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database implements AutoCloseable {

    private String url;
    private String user;
    private String password;
    private Connection connection;

    public Database(String url, String user, String password) {

        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() {

        try {
            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("Подключение к БД " + this.connection.getCatalog() + " установлено");
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
