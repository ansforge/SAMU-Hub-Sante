package com.hubsante.amqplab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LabRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(LabRunner.class);

  private final AmqpAdmin amqpAdmin;
  private final Producer producer;
  private final Consumer consumer;
  private final ChannelUsageProbe channelProbe;
  private final LabProperties props;

  private int exitCode = 0;
  private int publishChannels = 0;

  public LabRunner(
      AmqpAdmin amqpAdmin,
      Producer producer,
      Consumer consumer,
      ChannelUsageProbe channelProbe,
      LabProperties props) {
    this.amqpAdmin = amqpAdmin;
    this.producer = producer;
    this.consumer = consumer;
    this.channelProbe = channelProbe;
    this.props = props;
  }

  public int exitCode() {
    return exitCode;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    String queueName = props.queueName();
    log.info(
        "=== scenario '{}' | queue {} ({}) | messages={} producerThreads={} consumerConcurrency={} prefetch={} jitterMs={} requeueSeq={} useInvoke={}",
        props.getScenario(),
        queueName,
        props.getQueueType(),
        props.getMessages(),
        props.getProducerThreads(),
        props.getConsumerConcurrency(),
        props.getPrefetch(),
        props.getWorkJitterMs(),
        props.getRequeueSeq(),
        props.isUseInvoke());

    declareFreshQueue(queueName);

    int channelsBeforePublish = channelProbe.channelsCreated();
    Producer.PublishResult published = producer.publish(queueName, props);
    publishChannels = channelProbe.channelsCreated() - channelsBeforePublish;
    log.info(
        "publish phase opened {} new channel(s) (0 = reused the one cached channel)", publishChannels);
    awaitQueueDepth(queueName, props.getMessages());

    List<Integer> consumed = consumer.drain(queueName, props);

    report(published, consumed);
  }

  /**
   * {@code send()} is fire-and-forget without publisher confirms, so the broker may still be
   * enqueuing when the last call returns. Wait until the queue really holds every message, so the
   * drain phase measures the full queue and never races the tail of the publish phase.
   */
  private void awaitQueueDepth(String queueName, int expected) throws InterruptedException {
    int depth = producer.queueDepth(queueName);
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (depth < expected && System.nanoTime() < deadline) {
      Thread.sleep(50);
      depth = producer.queueDepth(queueName);
    }
    if (depth < expected) {
      log.error("only {}/{} messages reached the queue — messages were LOST, not reordered", depth, expected);
      exitCode = 1;
    } else {
      log.info("broker-reported queue depth before draining: {}", depth);
    }
  }

  /** Delete + declare so switching queue-type or rerunning never hits a stale/args mismatch. */
  private void declareFreshQueue(String queueName) {
    amqpAdmin.deleteQueue(queueName);
    Map<String, Object> queueArgs = new HashMap<>();
    if (props.isQuorum()) {
      queueArgs.put("x-queue-type", "quorum");
    }
    amqpAdmin.declareQueue(new Queue(queueName, true, false, false, queueArgs));
  }

  private void report(Producer.PublishResult published, List<Integer> consumed) {
    List<Integer> sequenceOrder = IntStream.range(0, props.getMessages()).boxed().toList();

    OrderAnalysis.Comparison vsSequence = OrderAnalysis.compare(consumed, sequenceOrder);
    OrderAnalysis.Comparison vsPublish = OrderAnalysis.compare(consumed, published.publishOrder());
    Map<String, OrderAnalysis.Comparison> perProducer =
        OrderAnalysis.comparePerProducer(consumed, published.publishOrder(), published.producerOfSeq());

    boolean complete = consumed.size() == props.getMessages();
    boolean preserved = vsPublish.preserved();
    boolean perProducerPreserved = perProducer.values().stream().allMatch(OrderAnalysis.Comparison::preserved);
    String verdict = preserved ? "ORDER PRESERVED" : "ORDER BROKEN";
    boolean asExpected =
        preserved == "preserved".equalsIgnoreCase(props.getExpect())
            && perProducerPreserved == "preserved".equalsIgnoreCase(props.getExpectPerProducer());

    StringBuilder out = new StringBuilder("\n");
    out.append("────────────────────────────────────────────────────────────────\n");
    out.append(String.format(" scenario            : %s%n", props.getScenario()));
    out.append(String.format(
        " new channels opened : %d during publish%s%n",
        publishChannels,
        publishChannels > props.getProducerThreads()
            ? String.format(
                "   <-- MORE CHANNELS THAN THREADS (~%d msg/channel): FIFO is only guaranteed per channel",
                props.getMessages() / publishChannels)
            : "   (reused cached channel(s))"));
    out.append(String.format(" published / consumed: %d / %d%s%n", props.getMessages(), consumed.size(),
        complete ? "" : "   <-- INCOMPLETE (drain timed out)"));
    out.append(String.format(
        " vs publish order    : %s  (out-of-order steps=%d, max displacement=%d)%n",
        preserved ? "identical" : "REORDERED", vsPublish.descents(), vsPublish.maxDisplacement()));
    out.append(String.format(
        " vs sequence 0..N-1  : %s  (out-of-order steps=%d, max displacement=%d)%n",
        vsSequence.preserved() ? "identical" : "REORDERED", vsSequence.descents(), vsSequence.maxDisplacement()));
    if (!vsPublish.preserved()) {
      out.append(String.format(" first inversion at  : index %d — %s%n", vsPublish.firstDescentAt(), vsPublish.detail()));
    }
    out.append(String.format(
        " per-producer order  : %s%n", perProducerPreserved ? "each thread's own messages stayed in order" : "BROKEN"));
    perProducer.forEach(
        (name, cmp) ->
            out.append(String.format("     %-12s : %s (steps=%d)%n", name, cmp.preserved() ? "in order" : "REORDERED",
                cmp.descents())));
    out.append(String.format(" VERDICT             : %s (expected %s -> %s)%n", verdict, props.getExpect(),
        asExpected ? "MATCH" : "MISMATCH"));
    out.append("────────────────────────────────────────────────────────────────");
    log.info(out.toString());

    // Machine-readable line for run-lab.sh's summary table.
    log.info(
        "LAB-RESULT|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}",
        props.getScenario(),
        props.getQueueType(),
        props.getMessages(),
        props.getProducerThreads(),
        props.getConsumerConcurrency(),
        props.getPrefetch(),
        publishChannels,
        consumed.size(),
        vsPublish.descents(),
        perProducerPreserved ? "per-producer-ok" : "per-producer-broken",
        verdict + (asExpected ? " (expected)" : " (UNEXPECTED)"));

    if (!complete || !asExpected) {
      exitCode = 1;
    }
  }
}
