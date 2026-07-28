package com.hubsante.amqplab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Drains the queue with a programmatically-started {@link SimpleMessageListenerContainer}
 * and records the order in which messages are delivered.
 *
 * <p>The container is started only <em>after</em> every message has been published, so with
 * {@code consumerConcurrency=1} and {@code prefetch=1} the recorded order is the queue's own
 * order — that is what answers "is the order preserved in the queue?". Raising concurrency or
 * prefetch turns the same recording into a measure of <em>processing</em> order instead.
 */
@Component
public class Consumer {

  private static final Logger log = LoggerFactory.getLogger(Consumer.class);

  private final ConnectionFactory connectionFactory;

  public Consumer(ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  /** @return the order in which sequence numbers were delivered to the listener. */
  public List<Integer> drain(String queueName, LabProperties props) throws InterruptedException {
    int total = props.getMessages();
    List<Integer> deliveryOrder = Collections.synchronizedList(new ArrayList<>(total));
    CountDownLatch latch = new CountDownLatch(total);
    AtomicBoolean requeueBudget = new AtomicBoolean(props.getRequeueSeq() >= 0);

    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(props.getConsumerConcurrency());
    container.setMaxConcurrentConsumers(props.getConsumerConcurrency());
    container.setPrefetchCount(props.getPrefetch());
    container.setAcknowledgeMode(AcknowledgeMode.AUTO);
    container.setDefaultRequeueRejected(true);
    container.setMessageListener(
        message -> {
          int seq = (Integer) message.getMessageProperties().getHeaders().get("seq");

          if (seq == props.getRequeueSeq() && requeueBudget.compareAndSet(true, false)) {
            // Reject-with-requeue exactly once: the message goes back to the head of the
            // queue, but anything already prefetched by this consumer is processed first.
            log.info("rejecting seq {} with requeue (once)", seq);
            throw new IllegalStateException("forced requeue of seq " + seq);
          }

          if (props.getWorkJitterMs() > 0) {
            try {
              Thread.sleep(seq % 2 == 0 ? props.getWorkJitterMs() : 0);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }

          deliveryOrder.add(seq);
          latch.countDown();
        });

    long start = System.nanoTime();
    container.start();
    boolean complete = latch.await(props.getDrainTimeoutSeconds(), TimeUnit.SECONDS);
    container.stop();
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    if (!complete) {
      log.error(
          "drain timed out after {}s: consumed {}/{}", props.getDrainTimeoutSeconds(), deliveryOrder.size(), total);
    } else {
      log.info(
          "consumed {} messages with {} consumer(s), prefetch {} in {} ms",
          deliveryOrder.size(),
          props.getConsumerConcurrency(),
          props.getPrefetch(),
          elapsedMs);
    }
    return new ArrayList<>(deliveryOrder);
  }
}
