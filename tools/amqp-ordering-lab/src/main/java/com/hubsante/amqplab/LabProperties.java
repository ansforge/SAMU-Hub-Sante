package com.hubsante.amqplab;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab")
public class LabProperties {

  /** Free-form label, only used in the report and in the queue name. */
  private String scenario = "baseline";

  /** Number of messages published. */
  private int messages = 2000;

  /** classic | quorum */
  private String queueType = "classic";

  /** Threads calling RabbitTemplate.send() concurrently. 1 = strictly sequential calls. */
  private int producerThreads = 1;

  /** SimpleMessageListenerContainer concurrentConsumers. */
  private int consumerConcurrency = 1;

  /** Consumer prefetch (basic.qos). */
  private int prefetch = 1;

  /** Body size; only affects realism, not ordering. */
  private int payloadBytes = 64;

  /** If > 0, even sequence numbers sleep this long in the listener, odd ones 0. */
  private int workJitterMs = 0;

  /** If >= 0, this sequence number is rejected-with-requeue exactly once. */
  private int requeueSeq = -1;

  private long drainTimeoutSeconds = 120;

  /** Publish inside {@code RabbitTemplate.invoke()}, which pins one channel per thread. */
  private boolean useInvoke = false;

  /**
   * Sleep between two {@code send()} calls. With publisher confirms this is the decisive knob: it
   * gives the broker's ack time to arrive, which lets the channel go back to the cache and be
   * reused instead of a new one being opened.
   */
  private int publishPauseMs = 0;

  public int getPublishPauseMs() {
    return publishPauseMs;
  }

  public void setPublishPauseMs(int publishPauseMs) {
    this.publishPauseMs = publishPauseMs;
  }

  /** preserved | broken — what the scenario is supposed to show for the global order. */
  private String expect = "preserved";

  /** preserved | broken — what it is supposed to show for each producer thread's own order. */
  private String expectPerProducer = "preserved";

  public boolean isUseInvoke() {
    return useInvoke;
  }

  public void setUseInvoke(boolean useInvoke) {
    this.useInvoke = useInvoke;
  }

  public String getExpectPerProducer() {
    return expectPerProducer;
  }

  public void setExpectPerProducer(String expectPerProducer) {
    this.expectPerProducer = expectPerProducer;
  }

  public String getScenario() {
    return scenario;
  }

  public void setScenario(String scenario) {
    this.scenario = scenario;
  }

  public int getMessages() {
    return messages;
  }

  public void setMessages(int messages) {
    this.messages = messages;
  }

  public String getQueueType() {
    return queueType;
  }

  public void setQueueType(String queueType) {
    this.queueType = queueType;
  }

  public int getProducerThreads() {
    return producerThreads;
  }

  public void setProducerThreads(int producerThreads) {
    this.producerThreads = producerThreads;
  }

  public int getConsumerConcurrency() {
    return consumerConcurrency;
  }

  public void setConsumerConcurrency(int consumerConcurrency) {
    this.consumerConcurrency = consumerConcurrency;
  }

  public int getPrefetch() {
    return prefetch;
  }

  public void setPrefetch(int prefetch) {
    this.prefetch = prefetch;
  }

  public int getPayloadBytes() {
    return payloadBytes;
  }

  public void setPayloadBytes(int payloadBytes) {
    this.payloadBytes = payloadBytes;
  }

  public int getWorkJitterMs() {
    return workJitterMs;
  }

  public void setWorkJitterMs(int workJitterMs) {
    this.workJitterMs = workJitterMs;
  }

  public int getRequeueSeq() {
    return requeueSeq;
  }

  public void setRequeueSeq(int requeueSeq) {
    this.requeueSeq = requeueSeq;
  }

  public long getDrainTimeoutSeconds() {
    return drainTimeoutSeconds;
  }

  public void setDrainTimeoutSeconds(long drainTimeoutSeconds) {
    this.drainTimeoutSeconds = drainTimeoutSeconds;
  }

  public String getExpect() {
    return expect;
  }

  public void setExpect(String expect) {
    this.expect = expect;
  }

  public boolean isQuorum() {
    return "quorum".equalsIgnoreCase(queueType);
  }

  public String queueName() {
    return "lab.order." + queueType.toLowerCase();
  }
}
