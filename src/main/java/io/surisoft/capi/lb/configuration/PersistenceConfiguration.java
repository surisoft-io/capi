package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.utils.Constants;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PersistenceConfiguration {

    @Value("${capi.persistence.enabled}")
    private boolean capiPersistenceEnabled;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${spring.jpa.properties.hibernate.dialect}")
    private String hibernateDialect;

    @Value("${spring.jpa.properties.driver.name}")
    private String driverName;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String hibernateDdlAuto;

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        if(capiPersistenceEnabled) {
            LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
            localContainerEntityManagerFactoryBean.setDataSource(dataSource());
            localContainerEntityManagerFactoryBean.setPackagesToScan(new String[] {Constants.SCHEMA_PACKAGES_TO_SCAN });

            JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            localContainerEntityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
            localContainerEntityManagerFactoryBean.setJpaProperties(additionalProperties());
            return localContainerEntityManagerFactoryBean;
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public DataSource dataSource() {
        if(capiPersistenceEnabled) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverName);
            dataSource.setUsername(datasourceUsername);
            dataSource.setPassword(datasourcePassword);
            dataSource.setUrl(datasourceUrl);
            return dataSource;
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public PlatformTransactionManager transactionManager() {
        if(capiPersistenceEnabled) {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return transactionManager;
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation(){
        if(capiPersistenceEnabled) {
            return new PersistenceExceptionTranslationPostProcessor();
        }
        return null;
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", hibernateDdlAuto);
        properties.setProperty("hibernate.dialect", hibernateDialect);
        return properties;
    }
}