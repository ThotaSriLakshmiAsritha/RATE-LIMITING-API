package com.example.ratelimiting;

import com.example.ratelimiting.ratelimit.TokenBucketRequest;
import com.example.ratelimiting.ratelimit.TokenBucketService;
import com.example.ratelimiting.testsupport.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TokenBucketConcurrencyTest extends IntegrationTestBase {
    @Autowired
    TokenBucketService tokenBucketService;

    @Test
    void redisLuaIsAtomicAcrossConcurrentConsumes() throws Exception {
        String key = "rate_limit:IP:test-concurrency:deadbeef";

        int capacity = 5;
        double refillPerSecond = 0.0001; // effectively no refill during test window
        int threads = 25;

        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    start.await();
                    return tokenBucketService.consume(new TokenBucketRequest(key, capacity, refillPerSecond, 1.0)).allowed();
                });
            }

            List<Future<Boolean>> futures = tasks.stream().map(pool::submit).toList();
            start.countDown();

            long allowed = 0;
            for (Future<Boolean> f : futures) {
                if (Boolean.TRUE.equals(f.get())) {
                    allowed++;
                }
            }

            org.assertj.core.api.Assertions.assertThat(allowed).isEqualTo(capacity);
        } finally {
            pool.shutdownNow();
        }
    }
}

