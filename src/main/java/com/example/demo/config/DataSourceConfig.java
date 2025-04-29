package com.example.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据源配置类
 */
@Configuration
public class DataSourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * 主数据源属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 从数据源属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSourceProperties slaveDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 主数据源
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.master.hikari")
    public DataSource masterDataSource() {
        logger.info("初始化主数据源...");
        DataSource dataSource = masterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        testConnection(dataSource, "Master DataSource");
        return dataSource;
    }

    /**
     * 从数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave.hikari")
    public DataSource slaveDataSource() {
        logger.info("初始化从数据源...");
        DataSource dataSource = slaveDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        testConnection(dataSource, "Slave DataSource");
        return dataSource;
    }

    /**
     * 路由数据源
     */
    @Bean
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        logger.info("配置路由数据源...");
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER, masterDataSource);
        targetDataSources.put(DataSourceType.SLAVE, slaveDataSource);
        
        logger.info("目标数据源数量: {}", targetDataSources.size());
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        
        return routingDataSource;
    }

    /**
     * 事务管理器
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource routingDataSource) {
        return new DataSourceTransactionManager(routingDataSource);
    }

    private void testConnection(DataSource dataSource, String dataSourceName) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection == null || connection.isClosed()) {
                logger.error("{}初始化失败: 连接为null或已关闭", dataSourceName);
                throw new IllegalStateException(dataSourceName + " initialization failed: Connection is null or closed");
            }
            logger.info("{}连接测试成功", dataSourceName);
        } catch (SQLException e) {
            logger.error("{}初始化失败: {}", dataSourceName, e.getMessage(), e);
            throw new IllegalStateException(dataSourceName + " initialization failed", e);
        }
    }
}
