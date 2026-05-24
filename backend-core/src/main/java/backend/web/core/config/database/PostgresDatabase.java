package backend.web.core.config.database;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ConditionalOnProperty(name = "backend.postgresdb.enabled", havingValue = "true")
@EntityScan(basePackages = "backend.web.core.model.entity")
@EnableJpaRepositories(basePackages = "backend.web.core.repository")
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
