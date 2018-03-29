package example.springcloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {
    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @Autowired
    UserRemote userRemote;

    @RequestMapping("/hi")
    public String hi() {
        return "Hello, " + userRemote.getName();
    }

    @RequestMapping("/hate")
    public String hate() throws InterruptedException {
        logger.info("--> hate");
        Thread.sleep(5000);
        throw new RuntimeException("greeting hate");
    }
}
