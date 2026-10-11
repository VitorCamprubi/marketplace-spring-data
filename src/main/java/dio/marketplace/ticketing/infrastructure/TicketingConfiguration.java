package dio.marketplace.ticketing.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.LinkedHashMap;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackages = "dio.marketplace.ticketing", entityManagerFactoryRef = "ticketingEntityManagerFactory", transactionManagerRef = "ticketingTransactionManager")
public class TicketingConfiguration {

    @Qualifier("ticketing")
    @Bean(defaultCandidate = false)
    @ConfigurationProperties("ticketing.datasource")
    public DataSourceProperties ticketingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Qualifier("ticketing")
    @Bean(defaultCandidate = false)
    @ConfigurationProperties("ticketing.datasource.configuration")
    public HikariDataSource ticketingDataSource(@Qualifier("ticketing") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Qualifier("ticketing")
    @Bean(defaultCandidate = false)
    @ConfigurationProperties("ticketing.jpa")
    public JpaProperties ticketingJpaProperties() {
        return new JpaProperties();
    }

    @Qualifier("ticketing")
    @Bean(defaultCandidate = false)
    public LocalContainerEntityManagerFactoryBean ticketingEntityManagerFactory(@Qualifier("ticketing") DataSource dataSource,
                                                                                @Qualifier("ticketing") JpaProperties jpaProperties) {
        var builder = new EntityManagerFactoryBuilder(
                new HibernateJpaVendorAdapter(),
                x -> new LinkedHashMap<>(jpaProperties.getProperties()),
                null
        );

        return builder
                .dataSource(dataSource)
                .packages("dio.marketplace.ticketing")
                .persistenceUnit("ticketing")
                .build();
    }

    @Qualifier("ticketing")
    @Bean
    public PlatformTransactionManager ticketingTransactionManager(@Qualifier("ticketing") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
