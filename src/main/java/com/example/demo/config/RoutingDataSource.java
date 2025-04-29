package com.example.demo.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 路由数据源，根据上下文决定使用哪个数据源
 */
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    /**
     * 确定当前查找键
     * @return 当前数据源的查找键
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
