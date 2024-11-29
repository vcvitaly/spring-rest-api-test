package io.github.vcvitaly.restapitest;

import java.time.Duration;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@RestController
public class TestController {

    private final Random random = new Random();
    private final int concurrency = Runtime.getRuntime().availableProcessors() * 2;

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

    @GetMapping("/hello-flux/{limit}")
    public Mono<String> helloFlux(@PathVariable Integer limit) {
        log.info("Calculating for limit: {}", limit);
        return Flux.fromIterable(IntStream.rangeClosed(1, limit).boxed().toList())
                .map(i -> processInt(i, limit))
                .retryWhen(
                        Retry
                                .backoff(3, Duration.ofSeconds(2))
                                .maxBackoff(Duration.ofMinutes(5))
                                .jitter(0.3)
                                .doBeforeRetry(signal -> log.warn("load assets: retrying attempt {}", signal.totalRetries() + 1))
                )
                .doOnError(t -> log.error("Error while processing ints: ", t))
                .parallel(concurrency)
                .runOn(Schedulers.newParallel("int-processing", concurrency))
                .collectSortedList(Comparator.comparingInt(Integer::intValue))
                .map(list -> String.valueOf(
                        list.stream().mapToInt(Integer::intValue).sum()
                ));
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
            log.error(message);
            throw new RuntimeException(message);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return i;
    }
}
