package io.github.vcvitaly.restapitest;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    private final Random random = new Random();
    private final int concurrency = concurrency(4, 8);
    private ForkJoinPool forkJoinPool = new ForkJoinPool(concurrency);

    @GetMapping("/hello")
    public CompletableFuture<String> helloWorld() {
        return CompletableFuture.supplyAsync(() -> {
            final String s = String.valueOf(get());
            if (log.isDebugEnabled()) {
                log.debug("Got {} from get()", s);
            }
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

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/hello-flux/{limit}")
    public String helloFlux(@PathVariable Integer limit) {
        final long start = System.nanoTime();
        log.info("Calculating for limit: {}", limit);

        final Integer res;
        try {
            res = forkJoinPool.submit(
                    () -> IntStream.rangeClosed(1, limit).boxed()
                            .parallel()
                            .map(i -> {
                                try {
                                    return processInt(i, limit);
                                } catch (Exception e) {
                                    log.error("Error while processing ints: ", e);
                                    return 0;
                                }
                            })
                            .mapToInt(Integer::intValue)
                            .sum()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        log.info(
                "process ints: Done. Elapsed time: {} ms", (System.nanoTime() - start) / 1_000_000
        );

        return String.valueOf(res);
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

    private int processInt(int i, int limit) {
        final String message = "Oops";
        if (random.nextInt(limit) == 0) {
            throw new RuntimeException(message);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Calculated for {}", i);
        return i;
    }

    private int concurrency(int lowerBound, int upperBound) {
        return Math.min(upperBound, Math.max(lowerBound, Runtime.getRuntime().availableProcessors() * 2));
    }
}
