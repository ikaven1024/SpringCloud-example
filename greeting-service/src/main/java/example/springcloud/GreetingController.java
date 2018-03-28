package example.springcloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {
    @Autowired
    UserRemote userRemote;

    @RequestMapping("/hi")
    public String hi() {
        return "Hello, " + userRemote.getName();
    }
}
