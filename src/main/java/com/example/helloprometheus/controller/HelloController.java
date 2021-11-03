package com.example.helloprometheus.controller;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController
public class HelloController {

  private final Map<String, byte[]> memory = new ConcurrentHashMap<>();

  @Autowired
  private PrometheusMeterRegistry meterRegistry;

  @PostConstruct
  private void init() {
    DistributionSummary.builder("timer_task").serviceLevelObjectives(3000D, 6000D).publishPercentileHistogram()
        .minimumExpectedValue(0.001D).maximumExpectedValue(10000D).publishPercentiles().register(meterRegistry);


    DistributionSummary timerTask = meterRegistry.summary("timer_task");
    Executors.newScheduledThreadPool(1)
        .scheduleAtFixedRate(() -> timerTask
                .record((ThreadLocalRandom.current().nextDouble(10D) * 1000))
            , 5, 1, TimeUnit.SECONDS);
  }

  @GetMapping("/hello")
  public String hello(@RequestParam(value = "name", defaultValue = "world") String name) {
    return String.format("Hello %s!", name);
  }

  @GetMapping("mem")
  public String mem() {
    memory.put("" + System.currentTimeMillis(), new byte[1024 * 1024]);
    return "total memory leak: " + memory.size() + "MB";
  }

  @GetMapping("fib")
  public String fib(@RequestParam(value = "n", defaultValue = "20") int n) {
    return String.format("fibonacci(%d) is %s", n, fibonacci(n));
  }

  private BigDecimal fibonacci(int i) {
    if (i <= 0) {
      return BigDecimal.ZERO;
    }
    if (i == 1) {
      return BigDecimal.ONE;
    }
    if (i == 2) {
      return BigDecimal.ONE;
    }
    return fibonacci(i - 1).add(fibonacci(i - 2));
  }

  @GetMapping("clear")
  public synchronized String clear() {
    int size = memory.size();
    memory.clear();
    System.gc();
    return size + "MB cleared";
  }
}
