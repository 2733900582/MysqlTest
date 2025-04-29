# MySQL主从复制Java应用示例

这个项目演示了如何在Java应用中实现MySQL主从复制的读写分离。

## 项目结构

```
demo/
├── src/main/java/com/example/demo/
│   ├── aspect/
│   │   └── DataSourceAspect.java        # 数据源切面，自动路由读写操作
│   ├── config/
│   │   ├── DataSourceConfig.java        # 数据源配置
│   │   ├── DataSourceContextHolder.java # 数据源上下文持有者
│   │   ├── DataSourceType.java          # 数据源类型枚举
│   │   └── RoutingDataSource.java       # 路由数据源
│   ├── controller/
│   │   └── UserController.java          # 用户REST控制器
│   ├── entity/
│   │   └── User.java                    # 用户实体类
│   ├── repository/
│   │   └── UserRepository.java          # 用户数据访问接口
│   ├── service/
│   │   ├── UserService.java             # 用户服务接口
│   │   └── impl/
│   │       └── UserServiceImpl.java     # 用户服务实现
│   └── DemoApplication.java             # 应用程序入口
├── src/main/resources/
│   └── application.yml                  # 应用配置文件
└── pom.xml                              # Maven项目配置
```

## 实现方式

本项目提供了两种实现MySQL主从复制读写分离的方式：

1. **Spring Boot自定义数据源方式**：使用AOP切面自动路由读写操作到不同的数据源
2. **Sharding-JDBC方式**：通过配置实现读写分离，无需编写额外代码

默认使用第一种方式，如需使用第二种方式，请修改`application.yml`文件中的配置。

## 如何运行

1. 确保MySQL主从复制集群已经搭建好，并且可以正常访问
2. 修改`application.yml`中的数据库连接信息
3. 运行应用程序：
   ```bash
   mvn spring-boot:run
   ```

## API接口

应用提供以下REST API接口：

- `GET /api/users` - 获取所有用户（读操作，使用从库）
- `GET /api/users/{id}` - 根据ID获取用户（读操作，使用从库）
- `GET /api/users/username/{username}` - 根据用户名获取用户（读操作，使用从库）
- `POST /api/users` - 创建用户（写操作，使用主库）
- `PUT /api/users/{id}` - 更新用户（写操作，使用主库）
- `DELETE /api/users/{id}` - 删除用户（写操作，使用主库）

## 测试

可以使用以下命令测试API接口：

```bash
# 创建用户（写操作，使用主库）
curl -X POST -H "Content-Type: application/json" -d '{"username":"user1","email":"user1@example.com","name":"User One"}' http://localhost:8080/api/users

# 获取所有用户（读操作，使用从库）
curl http://localhost:8080/api/users

# 根据ID获取用户（读操作，使用从库）
curl http://localhost:8080/api/users/1
```

## 注意事项

- 确保MySQL主从复制正常工作
- 主库用于写操作，从库用于读操作
- 事务操作会自动路由到主库
- 读操作方法名应以`get`、`find`、`select`、`query`、`list`或`count`开头
