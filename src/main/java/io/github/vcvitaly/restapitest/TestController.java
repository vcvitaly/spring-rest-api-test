package io.github.vcvitaly.restapitest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    private final Map<Long, Object> map = new ConcurrentHashMap<>();

    @GetMapping("/hello")
    public String helloWorld() {
        map.put(System.currentTimeMillis(), new Object());
        return String.valueOf(map.size());
    }

    private long get() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis();
    }
}
