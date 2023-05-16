package me.kiccco.plugin.mysql;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class MySQLManager {

    private final JavaPlugin main;
    private static HikariDataSource hikari;

    public MySQLManager(JavaPlugin main) {
        this.main = main;
        connect();
    }


    public void connect() {

        FileConfiguration config = main.getConfig();
        String ip = config.getString("mysql.hostname").split(":")[0];
        String port = config.getString("mysql.hostname").split(":")[1];
        String username = config.getString("mysql.user");
        String database = config.getString("mysql.database");
        String password = config.getString("mysql.password");

        hikari = new HikariDataSource();
        hikari.setMaximumPoolSize(10);
        hikari.setConnectionTimeout(250);
        hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", ip);
        hikari.addDataSourceProperty("port", port);
        hikari.addDataSourceProperty("databaseName", database);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);
    }


    public static void closeConnection()  {
        if(hikari != null)
            hikari.close();
    }

    public static void closeConnections(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
        try {
            if(connection!=null)
                connection.close();

            if (preparedStatement != null)
                preparedStatement.close();

            if(resultSet!=null)
                resultSet.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static HikariDataSource getHikari() {
        return hikari;
    }

    public static void createTable(String table) {
        try {
            Statement statement = hikari.getConnection().createStatement();
            statement.execute(table);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
