# 配置中心

## 配置仓库

在本地新建目录作为配置仓库，例如`E:/Projects/springcloud-demo/config-repo/`

仓库下添加`user-config-dev.properties`

```properties
user.name=Tom
```

添加`user-config-pro.properties`

```
user.name=Jerry
```

## 配置中心

新建工程`config-server`，pom添加

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-eureka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
        </dependency>
```

新建启动类

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

添加`application.properties`

```properties
spring.application.name=config-server
server.port=8791
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/,http://localhost:8762/eureka/

# 本地配置中心
spring.profiles.active=native
spring.cloud.config.server.native.search-locations=file:E:/Projects/springcloud-demo/config-repo/
```

启动工程访问  http://localhost:8791/user-config/dev 返回

```json
{"name":"user-config","profiles":["dev"],"label":null,"version":null,"state":null,"propertySources":[{"name":"file:E:/Projects/springcloud-demo/config-repo/user-config-dev.properties","source":{"user.name":"Tom"}}]}
```

访问 http://localhost:8791/user-config/pro 返回

```json
{"name":"user-config","profiles":["pro"],"label":null,"version":null,"state":null,"propertySources":[{"name":"file:E:/Projects/springcloud-demo/config-repo/user-config-pro.properties","source":{"user.name":"Jerry"}}]}
```

## 获取配置

修改`user-provider`，pom添加

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
```

新建资源文件`bootstrap.properties`

```properties
spring.cloud.config.profile=dev
spring.cloud.config.name=user-config
# git
spring.cloud.config.label=master
spring.cloud.config.discovery.enabled=true
# 注册中心在Eureka中的注册名称
spring.cloud.config.discovery.service-id=config-server
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/,http://localhost:8762/eureka/
```

>`bootstrap.properties`先于`application.properties`加载

访问 http://localhost:8771/users/name 得到

```
Tom
```

此时修改配置仓库中值，客户端并不能获取新值，除非向客户端POST bus/refresh。

## 消息总线

如果有多个服务从配置中心获取配置，每次都要挨个去执行刷新，显然是不合理的。这里，引入消息总线。

![](http://www.ityouknow.com/assets/images/2017/springcloud/configbus2.jpg)

### RabbitMQ

本地搭建RabbitMQ

### 配置中心添加消息总线

修改`config-server`的pom

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bus-amqp</artifactId>
        </dependency>
```

`application.properties`添加

```properties
# 消息总线
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
# 关闭安全验证
management.security.enabled=false
```

重启工程

### 服务添加消息总线

修改`user-provider`，pom添加

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bus-amqp</artifactId>
        </dependency>
```

`application.properties`添加

```properties
## 刷新时，关闭安全验证
management.security.enabled=false
## 开启消息跟踪
spring.cloud.bus.trace.enabled=true
# rabitmaq
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

重启工程

### 验证

修改配置仓库中的值，向配置中心发送refresh

```sh
curl -X POST http://localhost:8791/bus/refresh
```

重新访问 http://localhost:8771/users/name 。

*TODO：访问并没有变化，但是日志里已经看到refresh动作。待解决*