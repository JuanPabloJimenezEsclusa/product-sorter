package dev.jpje.productsorter.adapter.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VirtualThreadCountMetricsTest {

  private SimpleMeterRegistry registry;

  private VirtualThreadCountMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new VirtualThreadCountMetrics();
    metrics.bindTo(registry);
  }

  @AfterEach
  void tearDown() {
    metrics.destroy();
  }

  @Test
  void shouldCountParkedVirtualThreads() {
    final var queue = new SynchronousQueue<String>();
    final var started = new CountDownLatch(3);
    final var threads = new ArrayList<Thread>();

    for (int i = 0; i < 3; i++) {
      final var vt = Thread.ofVirtual().start(() -> {
        started.countDown();
        try {
          queue.take();
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
        }
      });
      threads.add(vt);
    }

    await().atMost(Duration.ofSeconds(5)).until(() -> started.getCount() == 0);

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      final var totalGauge = registry.get("jvm.threads.virtual.live")
        .tag("scheduling.status", "total").gauge();
      assertThat(totalGauge).as("virtual-thread total gauge registered").isNotNull();
      assertThat(totalGauge.value()).as("all parked virtual threads counted").isGreaterThanOrEqualTo(3);
    });

    for (Thread vt : threads) {
      vt.interrupt();
    }
  }

  @Test
  void shouldCountThreadsStartAndEnd() throws InterruptedException {
    final var started = new CountDownLatch(2);
    final var proceed = new CountDownLatch(1);
    final var threads = new ArrayList<Thread>();

    for (int i = 0; i < 2; i++) {
      final var vt = Thread.ofVirtual().start(() -> {
        started.countDown();
        try {
          proceed.await();
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
        }
      });
      threads.add(vt);
    }

    await().atMost(Duration.ofSeconds(5)).until(() -> started.getCount() == 0);

    final var totalGauge = registry.get("jvm.threads.virtual.live")
      .tag("scheduling.status", "total").gauge();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(totalGauge.value())
      .as("running virtual threads counted").isGreaterThanOrEqualTo(2));

    proceed.countDown();

    for (Thread vt : threads) {
      vt.join(Duration.ofSeconds(1));
    }

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
      assertThat(totalGauge.value()).as("gauge returns to zero once threads end").isZero());
  }
}
