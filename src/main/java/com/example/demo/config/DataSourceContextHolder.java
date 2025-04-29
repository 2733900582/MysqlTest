package com.example.demo.config;

/**
 * 数据源上下文持有者，用于存储当前线程使用的数据源类型
 */
public class DataSourceContextHolder {
    
    /**
     * 使用ThreadLocal存储当前线程的数据源类型
     */
    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();

    /**
     * 设置数据源类型
     * @param dataSourceType 数据源类型
     */
    public static void setDataSourceType(DataSourceType dataSourceType) {
        if (dataSourceType == null) {
            throw new IllegalArgumentException("Data source type cannot be null");
        }
        contextHolder.set(dataSourceType);
    }

    /**
     * 获取数据源类型，如果未设置则默认返回主库
     * @return 数据源类型
     */
    public static DataSourceType getDataSourceType() {
        return contextHolder.get() != null ? contextHolder.get() : DataSourceType.MASTER;
    }

    /**
     * 清除数据源类型
     */
    public static void clearDataSourceType() {
        contextHolder.remove();
    }
}
