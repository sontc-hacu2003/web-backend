package backend.web.core.config.database;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ConditionalOnProperty(name = "backend.postgresdb.enabled", havingValue = "true")
public class PostgresDatabase extends Database {
    public static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";

    public PostgresDatabase(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password
    ) {
        super(url, username, password);
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    @Primary
    @Bean(name = "postgresDataSource")
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(getUrl());
        dataSource.setUsername(getUsername());
        dataSource.setPassword(getPassword());
        dataSource.setDriverClassName(getDriverClassName());
        return dataSource;
    }
}
