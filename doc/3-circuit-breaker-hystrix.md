# 熔断器Hystrix

熔断器作用于服务调用端，可以在远程服务不可用时候瞬速返回，避免了服务级联错误引发雪崩。

## 实践

修改上节中的`greeting-service`工程。

`application.properties`中开启熔断：

```properties
feign.hystrix.enabled=true
```

创建Fallback回调类

```java
@Component
public class UserRemoteHystrix implements UserRemote {
    @Override
    public String getName() {
        return "unknown name";
    }
}
```

远程调用接口添加Fallback

```java
@FeignClient(name = "user-provider", fallback = UserRemoteHystrix.class)
```

重新启动项目，访问 http://localhost:8775/hi ，依旧返回

```
Hello, Tom
```

关闭`User Provider`工程，再次访问，返回

```
Hello, unknown name
```





