package io.github.vcvitaly.restapitest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/bytes")
public class ByteController {

    @GetMapping("/hello")
    public Dto helloBytes() {
        return new Dto("Hello".getBytes());
    }
}
