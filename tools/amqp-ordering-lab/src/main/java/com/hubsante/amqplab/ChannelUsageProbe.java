package com.hubsante.amqplab;

import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ChannelListener;
import org.springframework.stereotype.Component;

/**
 * Counts how many AMQP channels the publishing path actually creates.
 *
 * <p>This is the key measurement of the lab: AMQP only guarantees FIFO ordering
 * <em>per channel</em>. {@code RabbitTemplate.send()} borrows a channel from the
 * {@link CachingConnectionFactory} cache and returns it afterwards, so it is not
 * thread-affine — and when publisher confirms are enabled, a channel with unconfirmed
 * messages is <em>not</em> returned to the cache, which makes a tight publish loop churn
 * through many channels. If this counter is > 1 for the publish phase, ordering is not
 * guaranteed by the protocol.
 */
@Component
public class ChannelUsageProbe implements ChannelListener {

  private final CachingConnectionFactory connectionFactory;
  private final AtomicInteger created = new AtomicInteger();
  private final Set<Integer> channelNumbers = ConcurrentHashMap.newKeySet();

  public ChannelUsageProbe(CachingConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  @PostConstruct
  void register() {
    connectionFactory.addChannelListener(this);
  }

  @Override
  public void onCreate(Channel channel, boolean transactional) {
    created.incrementAndGet();
    channelNumbers.add(channel.getChannelNumber());
  }

  public int channelsCreated() {
    return created.get();
  }

  public int distinctChannelNumbers() {
    return channelNumbers.size();
  }
}
