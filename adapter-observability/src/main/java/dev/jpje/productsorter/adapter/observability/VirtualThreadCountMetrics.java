package dev.jpje.productsorter.adapter.observability;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import jdk.jfr.consumer.RecordingStream;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.DisposableBean;

public class VirtualThreadCountMetrics implements MeterBinder, DisposableBean {

  private static final String START_EVENT = "jdk.VirtualThreadStart";

  private static final String END_EVENT = "jdk.VirtualThreadEnd";

  private final AtomicLong liveCount = new AtomicLong(0);

  private RecordingStream recordingStream;

  @Override
  public void bindTo(final @NonNull MeterRegistry registry) {
    recordingStream = new RecordingStream();
    recordingStream.enable(START_EVENT);
    recordingStream.enable(END_EVENT);
    recordingStream.setMaxAge(Duration.ofSeconds(5));
    recordingStream.setMaxSize(10L * 1024 * 1024);
    recordingStream.onEvent(START_EVENT, _ -> liveCount.incrementAndGet());
    recordingStream.onEvent(END_EVENT, _ -> liveCount.decrementAndGet());
    recordingStream.startAsync();

    Gauge.builder("jvm.threads.virtual.live", liveCount, AtomicLong::doubleValue)
      .tag("scheduling.status", "total")
      .baseUnit(BaseUnits.THREADS)
      .description("Total number of virtual threads that have started but not yet terminated")
      .register(registry);
  }

  @Override
  public void destroy() {
    if (recordingStream != null) {
      recordingStream.close();
    }
  }
}
