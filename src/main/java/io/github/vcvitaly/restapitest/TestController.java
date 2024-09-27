package io.github.vcvitaly.restapitest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @GetMapping("/hello")
    public CompletableFuture<String> helloWorld() {
        return CompletableFuture.supplyAsync(() -> {
            final String s = String.valueOf(get());
            log.info("Got {} from get()", s);
            return s;
        });
    }

    @GetMapping("/hello-parallel")
    public String helloWorldParallel() {
        try (ForkJoinPool fjp = new ForkJoinPool(4)) {
            return fjp.submit(
                    () -> IntStream.range(0, 12).parallel().mapToObj(this::intToString).collect(Collectors.joining(","))
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private long get() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis();
    }

    private String intToString(int i) {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Converting {} to string on a thread {}", i, Thread.currentThread().getName());
        return String.valueOf(i);
    }
}
