package example.springcloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RefreshScope // 开启配置刷新
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${account.user.name}")
    private String name;

    @RequestMapping("/name")
    public String getName() {
        logger.info("+++ Get name");
        return name;
    }
}
