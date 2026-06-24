package za.co.capitecbank.transaction_event_consumer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "velocity.redis.enabled", havingValue = "true", matchIfMissing = false)
public class VelocityFallbackDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "velocity.fallback.datasource")
    public HikariConfig velocityFallbackHikariConfig() {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("velocity-fallback");
        cfg.setConnectionTimeout(2000);
        cfg.setMaximumPoolSize(3);
        return cfg;
    }

    @Bean
    public DataSource velocityFallbackDataSource(HikariConfig velocityFallbackHikariConfig) {
        return new HikariDataSource(velocityFallbackHikariConfig);
    }

    @Bean("velocityFallbackJdbc")
    public JdbcTemplate velocityFallbackJdbc(DataSource velocityFallbackDataSource) {
        return new JdbcTemplate(velocityFallbackDataSource);
    }
}
