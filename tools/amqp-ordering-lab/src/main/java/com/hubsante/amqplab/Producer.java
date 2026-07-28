package com.hubsante.amqplab;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code messages} sequence-numbered messages to the queue via the default
 * exchange, using {@code producerThreads} threads.
 *
 * <p>Two orders are recorded:
 *
 * <ul>
 *   <li>the <b>sequence</b> order (0..N-1), assigned before the send call;
 *   <li>the <b>publish-completion</b> order, appended after {@code send()} returns.
 * </ul>
 *
 * With a single thread these are identical. With several threads they diverge, which
 * lets the report distinguish "the broker/Spring reordered my messages" from "my own
 * threads raced before reaching the broker".
 */
@Component
public class Producer {

  private static final Logger log = LoggerFactory.getLogger(Producer.class);

  private final RabbitTemplate rabbitTemplate;

  public Producer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * @param publishOrder sequence numbers in the order their {@code send()} call returned
   * @param producerOfSeq index = sequence number, value = name of the publishing thread
   */
  public record PublishResult(List<Integer> publishOrder, String[] producerOfSeq) {}

  public PublishResult publish(String queueName, LabProperties props) throws InterruptedException {
    int total = props.getMessages();
    int threads = props.getProducerThreads();
    byte[] filler = new byte[Math.max(0, props.getPayloadBytes() - 16)];
    Arrays.fill(filler, (byte) '.');
    String fillerString = new String(filler, StandardCharsets.UTF_8);

    List<Integer> publishOrder = Collections.synchronizedList(new ArrayList<>(total));
    String[] producerOfSeq = new String[total];
    AtomicInteger nextSeq = new AtomicInteger(0);
    CountDownLatch done = new CountDownLatch(threads);

    long start = System.nanoTime();
    for (int t = 0; t < threads; t++) {
      String threadName = "producer-" + t;
      Thread worker =
          new Thread(
              () -> {
                try {
                  if (props.isUseInvoke()) {
                    // Variant: RabbitOperations.invoke() pins ONE channel to this thread for
                    // the whole scope, so every send below goes over the same channel.
                    rabbitTemplate.invoke(
                        operations -> {
                          publishLoop(
                              queueName, props, nextSeq, producerOfSeq, publishOrder, threadName,
                              fillerString, operations::send);
                          if (rabbitTemplate.getConnectionFactory().isPublisherConfirms()) {
                            operations.waitForConfirms(30_000);
                          }
                          return null;
                        });
                  } else {
                    // THE CALL UNDER TEST: a plain, sequential RabbitTemplate publish. Each
                    // call independently borrows a channel from the connection factory cache.
                    publishLoop(
                        queueName, props, nextSeq, producerOfSeq, publishOrder, threadName,
                        fillerString, rabbitTemplate::send);
                  }
                } catch (Exception e) {
                  log.error("producer {} failed", threadName, e);
                } finally {
                  done.countDown();
                }
              },
              threadName);
      worker.start();
    }
    done.await();

    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    log.info("published {} messages with {} thread(s) in {} ms", total, threads, elapsedMs);
    return new PublishResult(new ArrayList<>(publishOrder), producerOfSeq);
  }

  /** Either {@code RabbitTemplate::send} (channel per call) or {@code operations::send} (pinned channel). */
  private interface SendOp {
    void send(String exchange, String routingKey, Message message);
  }

  private void publishLoop(
      String queueName,
      LabProperties props,
      AtomicInteger nextSeq,
      String[] producerOfSeq,
      List<Integer> publishOrder,
      String threadName,
      String fillerString,
      SendOp sender) {
    int total = props.getMessages();
    int pauseMs = props.getPublishPauseMs();
    int seq;
    while ((seq = nextSeq.getAndIncrement()) < total) {
      producerOfSeq[seq] = threadName;
      MessageProperties mp = new MessageProperties();
      mp.setHeader("seq", seq);
      mp.setHeader("producer", threadName);
      mp.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
      mp.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
      byte[] body = (seq + "|" + threadName + "|" + fillerString).getBytes(StandardCharsets.UTF_8);

      sender.send("", queueName, new Message(body, mp));

      publishOrder.add(seq);

      if (pauseMs > 0) {
        try {
          Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  /** Broker-side depth, read on a fresh channel — proves the messages are really queued. */
  public int queueDepth(String queueName) {
    Integer count = rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName).getMessageCount());
    return count == null ? -1 : count;
  }
}
