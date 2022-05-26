package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class PersistenceConfiguration {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String hibernateDdlAuto;

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setDataSource(dataSource());
        localContainerEntityManagerFactoryBean.setPackagesToScan(new String[] {Constants.SCHEMA_PACKAGES_TO_SCAN });

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(additionalProperties());
        return localContainerEntityManagerFactoryBean;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(discoverDriver());
        dataSource.setUsername(datasourceUsername);
        dataSource.setPassword(datasourcePassword);
        dataSource.setUrl(datasourceUrl);
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation(){
       return new PersistenceExceptionTranslationPostProcessor();
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", hibernateDdlAuto);
        properties.setProperty("hibernate.dialect", discoverDialect());
        return properties;
    }

    private String discoverDialect() {
        if(datasourceUrl.contains("mysql")) {
            return "org.hibernate.dialect.MySQL55Dialect";
        } else if(datasourceUrl.contains("h2")) {
            return "org.hibernate.dialect.H2Dialect";
        } else if(datasourceUrl.contains("postgres")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else {
            return null;
        }
    }

    private String discoverDriver() {
        if(datasourceUrl.contains("mysql")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if(datasourceUrl.contains("h2")) {
            return "org.h2.Driver";
        } else if(datasourceUrl.contains("postgres")) {
            return "org.postgresql.Driver";
        } else {
            return null;
        }
    }
}