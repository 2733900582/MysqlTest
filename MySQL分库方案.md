# MySQL主从复制基础上添加分库方案

本文档提供在现有MySQL主从复制集群基础上添加分库功能的方案建议。

## 分库方案选择

1. **Sharding-JDBC (ShardingSphere-JDBC)**
   - 轻量级Java框架，零侵入
   - 可以与现有的主从复制架构无缝集成
   - 在应用层面实现分库分表，不需要额外的代理服务器

2. **MyCat**
   - 独立的数据库中间件，以代理方式运行
   - 支持分库分表和读写分离
   - 对应用透明，应用只需连接到MyCat

3. **ShardingSphere-Proxy**
   - 提供数据库服务端，应用连接到代理即可
   - 支持异构语言，不限于Java应用
   - 完全兼容MySQL协议

4. **MySQL Router + MySQL InnoDB Cluster**
   - 官方解决方案，可以与Group Replication结合
   - 提供自动故障转移和负载均衡

## 对现有主从集群的影响

添加分库组件对现有主从复制集群的影响：

- **数据结构变更**：需要按照分片键重新组织数据
- **配置调整**：主从复制配置可能需要调整以适应分库架构
- **性能影响**：短期内可能有性能波动，但长期会提高系统吞吐量
- **事务处理**：跨分片事务需要特别处理，可能增加复杂性

## 推荐方案：ShardingSphere-JDBC

基于已有的Java应用和MySQL主从复制架构，推荐使用**ShardingSphere-JDBC**：

1. 它可以无缝集成到现有的Spring Boot应用中
2. 不需要额外的代理服务器，减少网络开销
3. 可以保留现有的主从复制架构，只需添加分片配置
4. 与现有项目技术栈兼容性好

## 实施步骤

### 1. 添加依赖

在`pom.xml`中添加ShardingSphere-JDBC依赖：

```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core</artifactId>
    <version>5.3.2</version>
</dependency>
```

### 2. 配置分片规则

在`application.yml`中添加ShardingSphere配置，同时保留主从复制配置：

```yaml
spring:
  shardingsphere:
    datasource:
      names: master,slave1,slave2
      master:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://master-host:3306/db
        username: root
        password: password
      slave1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://slave1-host:3306/db
        username: root
        password: password
      slave2:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://slave2-host:3306/db
        username: root
        password: password
    rules:
      readwrite-splitting:
        data-sources:
          readwrite_ds:
            type: Static
            props:
              write-data-source-name: master
              read-data-source-names: slave1,slave2
      sharding:
        tables:
          user:  # 用户表分片配置
            actual-data-nodes: readwrite_ds.user_${0..1}
            table-strategy:
              standard:
                sharding-column: id
                sharding-algorithm-name: user_inline
        sharding-algorithms:
          user_inline:
            type: INLINE
            props:
              algorithm-expression: user_${id % 2}
    props:
      sql-show: true
```

### 3. 修改现有代码

1. 移除自定义的数据源路由逻辑：
   - 不再需要`DataSourceAspect.java`
   - 不再需要`DataSourceContextHolder.java`
   - 不再需要`DataSourceType.java`
   - 不再需要`RoutingDataSource.java`

2. 修改`DataSourceConfig.java`以使用ShardingSphere提供的数据源：

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource dataSource() throws SQLException {
        return ShardingSphereDataSourceFactory.createDataSource();
    }
}
```

### 4. 数据库准备

1. 在每个数据库节点上创建分片表：

```sql
-- 在所有数据库节点上执行
CREATE TABLE user_0 (
  id BIGINT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL,
  name VARCHAR(100) NOT NULL
);

CREATE TABLE user_1 (
  id BIGINT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL,
  name VARCHAR(100) NOT NULL
);
```

2. 数据迁移：将现有数据按照分片规则迁移到新表中

## 注意事项

1. **分片键选择**：选择合适的分片键对性能至关重要，通常选择高基数、分布均匀的列
2. **数据迁移**：需要制定详细的数据迁移计划，确保数据一致性
3. **应用改造**：可能需要调整部分SQL查询以适应分片架构
4. **监控与维护**：添加对分片数据库的监控，确保性能和可用性
5. **备份策略**：调整备份策略以适应分片架构

## 扩展阅读

- [ShardingSphere官方文档](https://shardingsphere.apache.org/document/current/cn/overview/)
- [MySQL分库分表最佳实践](https://dev.mysql.com/doc/refman/8.0/en/sharding.html)
