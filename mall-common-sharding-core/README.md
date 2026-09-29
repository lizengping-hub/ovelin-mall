# 依赖说明，shardingsphere 5.5.3
## 1. 到达基础编译通过，配置文件中几乎为空
```asciidoc
dataSources:
rules:
props
```
```asciidoc
shardingsphere-jdbc                                # 核心入口
shardingsphere-standalone-mode-core                # standalone 模式
shardingsphere-standalone-mode-repository-memory   # 元数据存内存
shardingsphere-authority-simple                    # 默认权限实现
```
## 2. 配置一个数据源
```asciidoc
dataSources:
  ds0:
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/user_database?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    username: ovelin
    password: ovelin
```
```asciidoc
HikariCP + shardingsphere-infra-data-source-pool-hikari + mysql-connector-j  # 引入真实数据源后追加
```
## 3. 配置分片规则
```asciidoc
- !SHARDING
  tables:
```
```asciidoc
shardingsphere-sharding-core                       # !SHARDING 规则
```
## 4. 运行时真正能解析sql正确执行
```asciidoc
shardingsphere-jdbc-dialect-mysql     # MySQL 方言支持（运行时必需）
```
