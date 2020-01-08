/*
 * Copyright (c) 2020 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.demos.springtomcatthreads;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
@Slf4j
class IndexController {
    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    String index() {
        final var threadName = Thread.currentThread().getName();
        log.info("Handling request in thread {}", threadName);

        // Run a CPU intensive task.
        for (int i = 0; i < 10_000_000; ++i) {
            final double j = Math.pow(i, 2);
            log.debug("Result: {}", j);
        }

        return "Thread: " + threadName;
    }
}

@RestController
class SystemInfoController {
    @GetMapping(value = "/systeminfo", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> systemInfo() throws UnknownHostException {
        final var heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return Map.of(
                "availableProcessors", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(),
                "javaVersion", System.getProperty("java.version"),
                "threadCount", ManagementFactory.getThreadMXBean().getThreadCount(),
                "hostName", InetAddress.getLocalHost().getCanonicalHostName()
        );
    }
}

@Configuration
class MetricsConfig {
    @Bean
    Gauge runnableThreadCount(MeterRegistry registry) {
        return Gauge.builder("jvm.threads.runnable", this::computeRunnableThreadCount)
                .baseUnit("threads")
                .description("The number of threads in state RUNNABLE")
                .register(registry);
    }

    private Number computeRunnableThreadCount() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getState().equals(Thread.State.RUNNABLE))
                .count();
    }
}
