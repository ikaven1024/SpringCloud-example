# 服务网关 Zuul

## 准备

`GreetingController`添加模拟业务错误

```java
@RequestMapping("/hate")
public String hate() throws InterruptedException {
    logger.info("--> hate");
    Thread.sleep(5000);
    throw new RuntimeException("greeting hate");
}
```

## 搭建服务网关

新建工程，pom添加

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-eureka</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-zuul</artifactId>
</dependency>
```

添加启动类

```java
@SpringBootApplication
@EnableZuulProxy
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

添加`application.properties`

```properties
spring.application.name=gateway
server.port=80
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/,http://localhost:8762/eureka/

#这里的配置表示，访问/it/** 直接重定向到http://www.ityouknow.com/**
zuul.routes.greeting.path=/greeting/**
zuul.routes.greeting.serviceId=greeting-service
```

启动工程，访问 http://localhost/greeting/hi ，返回

```
Hello, Tom
```

## 过滤器

Filter是Zuul的核心，用来实现对外服务的控制。Filter的生命周期有4个，分别是“PRE”、“ROUTING”、“POST”、“ERROR”，整个生命周期可以用下图来表示。

![](http://www.ityouknow.com/assets/images/2018/springcloud/zuul-core.png)Zuul大部分功能都是通过过滤器来实现的，这些过滤器类型对应于请求的典型生命周期。

- **PRE：** 这种过滤器在请求被路由之前调用。我们可利用这种过滤器实现身份验证、在集群中选择请求的微服务、记录调试信息等。
- **ROUTING：**这种过滤器将请求路由到微服务。这种过滤器用于构建发送给微服务的请求，并使用Apache HttpClient或Netfilx Ribbon请求微服务。
- **POST：**这种过滤器在路由到微服务以后执行。这种过滤器可用来为响应添加标准的HTTP Header、收集统计信息和指标、将响应从微服务发送给客户端等。
- **ERROR：**在其他阶段发生错误时执行该过滤器。 除了默认的过滤器类型，Zuul还允许我们创建自定义的过滤器类型。例如，我们可以定制一种STATIC类型的过滤器，直接在Zuul中生成响应，而不将请求转发到后端的微服务。

### 实践

新建`PreFilter`

```java
@Component
class PreFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(PreFilter.class);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        logger.info("-->>> {} {}", request.getMethod(), request.getRequestURL().toString());
        return null;
    }
}
```

新建`PostFilter`

```java
@Component
class PostFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(PreFilter.class);

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();

        logger.info("<<<-- {} {}, {}", request.getMethod(), request.getRequestURL().toString(), response.getStatus());
        return null;
    }
}
```

重启工程后再访问 http://localhost/greeting/hi 。观察工程日志出现

```
-->>> GET http://localhost/greeting/hi
<<<-- GET http://localhost/greeting/hi, 200
```

## 路由熔断

添加

```java
@Component
public class GreetingServiceFallback implements ZuulFallbackProvider {
    @Override
    public String getRoute() {
        return "greeting-service";
    }

    @Override
    public ClientHttpResponse fallbackResponse() {
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return 200;
            }

            @Override
            public String getStatusText() throws IOException {
                return "OK";
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream("User provider is unavailable".getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_HTML);
                return headers;
            }
        };
    }
}
```

重启项目，访问 http://localhost/greeting/hate ，得到

```
User provider is unavailable
```

## 路由重试

pom添加

```xml
<dependency>
	<groupId>org.springframework.retry</groupId>
	<artifactId>spring-retry</artifactId>
</dependency>
```

`application.properties`添加

```properties
#是否开启重试功能
zuul.retryable=true
#对当前服务的重试次数
ribbon.MaxAutoRetries=2
#切换相同Server的次数
ribbon.MaxAutoRetriesNextServer=0
```

重新工程，访问 http://localhost/greeting/hate ，观察`greeting-service`的日志，看到收到两次请求。