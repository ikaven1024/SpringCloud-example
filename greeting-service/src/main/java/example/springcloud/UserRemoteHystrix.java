package example.springcloud;

import org.springframework.stereotype.Component;

@Component
public class UserRemoteHystrix implements UserRemote {

    @Override
    public String getName() {
        return "unknown name";
    }
}
