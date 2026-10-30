package com.nexturn.lms.aws;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

/** Shared JDBC wiring for small infrastructure handlers, using injected database secrets. */
public final class DatabaseConnection {
    private DatabaseConnection() { }
    public static DataSource dataSource() {
        return new DriverManagerDataSource(required("DB_URL"),required("DB_USERNAME"),required("DB_PASSWORD"));
    }
    public static JdbcTemplate jdbc() { return new JdbcTemplate(dataSource()); }
    public static String required(String name) {
        String value=System.getenv(name);
        if(value==null || value.isBlank()) throw new IllegalStateException("Missing environment setting: "+name);
        return value;
    }
}
