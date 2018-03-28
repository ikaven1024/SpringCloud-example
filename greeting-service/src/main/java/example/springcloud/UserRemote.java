package example.springcloud;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "user-provider", fallback = UserRemoteHystrix.class) // 与远程服务名称一致
public interface UserRemote {
    @RequestMapping("/users/name")   // 远程服务路径
    String getName();
}
