package tnr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import static tnr.Constants.TLS_PROTOCOL_VERSION;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;

import io.github.cdimascio.dotenv.Dotenv;
import tnr.dto.MessageDTO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AMQPTestSupport {
    protected final Map<String, Collection<String>> config = new HashMap<>(Map.of(
            "15-15_v1.5", List.of("fr.health.tnr.samu1-v1", "fr.health.tnr.samu2-v1"),
            "15-15_v2.0", List.of("fr.health.tnr.samu1-v2", "fr.health.tnr.samu2-v2"),
            "15-15_v2.1", List.of("fr.health.tnr.samu1-v3", "fr.health.tnr.samu2-v3"),
            "15-nexsis_v1.9", List.of("fr.fire.tnr.sdis")
    ));

    protected static final String BASE_URL = "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/refs/tags";

    protected Map<String, Producer> producers = new HashMap<>();
    protected Collection<TestConsumer> consumers = new ArrayList<>();

    protected static final int RECEIVE_TIMEOUT_SECS = 10;

    protected Dotenv dotenv = Dotenv.load();

    HttpClientWithCache httpClient = new HttpClientWithCache(dotenv.get("GITHUB_TOKEN"));

    // global message collector
    protected final MessageCollector inbox = new MessageCollector();

    @BeforeAll
    void setUpAll() throws Exception {
        TLSConf tlsConf = new TLSConf(
                TLS_PROTOCOL_VERSION,
                dotenv.get("KEY_PASSPHRASE"),
                dotenv.get("CERTIFICATE_PATH"),
                dotenv.get("TRUST_STORE_PASSWORD"),
                dotenv.get("TRUST_STORE_PATH")
        );

        String host = dotenv.get("HUB_HOSTNAME");
        int port = Integer.parseInt(dotenv.get("HUB_PORT"));
        String exchange = dotenv.get("EXCHANGE_NAME");

        for (Map.Entry<String, Collection<String>> entry : config.entrySet()) {
            String vhost = entry.getKey();
            Collection<String> clients = entry.getValue();
            producers.put(vhost, createProducer(host, port, vhost, exchange, tlsConf));
            for (String client : clients) {
                consumers.add(
                        createConsumer(host, port, vhost, exchange, client, client + ".message", inbox, tlsConf)
                );
                consumers.add(
                        createConsumer(host, port, vhost, exchange, client, client + ".ack", inbox, tlsConf));
            }
        }
    }

    @AfterAll
    void tearDownAll() throws Exception {
        consumers.forEach(consumer -> {
            try {
                consumer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        producers.values().forEach(producer -> {
            try {
                producer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @BeforeEach
    void clearInbox() {
        inbox.clear();
    }

    protected String getUseCaseContentOnline(String tagRef, String fileRef) {
        String refUrl = String.format("%s/%s/src/main/resources/sample/examples/%s", BASE_URL, tagRef, fileRef);
        return httpClient.fetch(refUrl);
    }

    protected MessageDTO awaitMessage(String distributionId) throws Exception {
        return inbox.awaitMessage(distributionId, RECEIVE_TIMEOUT_SECS, TimeUnit.SECONDS);
    }

    protected void sendMessage(String vhost, String routingKey, String message) throws Exception {
        Producer producer = getProducer(vhost);
        producer.publish(routingKey, message.getBytes());
    }

    String sendAck(String vhost, String routingKey, String recipientId, String referencedDistributionId) throws Exception {
        String ackDistributionId = Utils.generateDistributionId(routingKey);
        String refMessage = new AckBuilder().buildAck(routingKey, recipientId, ackDistributionId, referencedDistributionId);
        sendMessage(vhost, routingKey, refMessage);
        return ackDistributionId;
    }

    private Producer getProducer(String vhost) {
        return producers.get(vhost);
    }

    private Producer createProducer(String host, int port, String vhost, String exchange, TLSConf tlsConf) throws Exception {
        Producer p = new Producer(host, port, vhost, exchange);
        p.connect(tlsConf);
        return p;
    }

    private TestConsumer createConsumer(String host, int port, String vhost, String exchange,
            String clientId, String queueName, MessageCollector inbox, TLSConf tlsConf) throws Exception {
        TestConsumer c = new TestConsumer(host, port, vhost, exchange, queueName, clientId, inbox);
        c.connect(tlsConf);
        return c;
    }


    static class MessageCollector {

        private final ConcurrentHashMap<String, CompletableFuture<MessageDTO>> pending = new ConcurrentHashMap<>();
        private final List<MessageDTO> buffer = new ArrayList<>();
        private final ReentrantLock lock = new ReentrantLock();
        ObjectMapper mapper = new ObjectMapper();

        void add(MessageDTO delivery) {
            lock.lock();
            try {
                for (Iterator<Map.Entry<String, CompletableFuture<MessageDTO>>> it = pending.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, CompletableFuture<MessageDTO>> entry = it.next();
                    if (delivery.getDistributionId().equals(entry.getKey())) {
                        it.remove();
                        entry.getValue().complete(delivery);
                        return;
                    }
                }
                buffer.add(delivery);
            } catch (Exception e) {
                buffer.add(delivery);
            } finally {
                lock.unlock();
            }
        }

        MessageDTO awaitMessage(String distributionId, long timeout, TimeUnit unit) throws Exception {
            CompletableFuture<MessageDTO> future;
            lock.lock();
            try {
                for (Iterator<MessageDTO> it = buffer.iterator(); it.hasNext();) {
                    MessageDTO message = it.next();
                    if (message.getDistributionId().equals(distributionId)) {
                        it.remove();
                        return message;
                    }
                }
                future = new CompletableFuture<>();
                pending.put(distributionId, future);
            } finally {
                lock.unlock();
            }
            try {
                return future.get(timeout, unit);
            } catch (TimeoutException e) {
                lock.lock();
                try {
                    pending.remove(distributionId, future);
                } finally {
                    lock.unlock();
                }
                throw new Exception("Could not receive message that matched " + distributionId);
            }
        }

        void clear() {
            lock.lock();
            try {
                buffer.clear();
                for (CompletableFuture<MessageDTO> f : pending.values()) {
                    f.cancel(false);
                }
                pending.clear();
            } finally {
                lock.unlock();
            }
        }
    }

    static class TestConsumer extends Consumer {

        private final MessageCollector inbox;

        TestConsumer(String host, int port, String vhost, String exchangeName,
                String queueName, String clientId, MessageCollector inbox) {
            super(host, port, vhost, exchangeName, queueName, clientId);
            this.inbox = inbox;
        }

        @Override
        protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
            consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            try {
                MessageDTO message = new MessageDTO(this.vhost, this.queueName, delivery);
                inbox.add(message);

            } catch (Exception e) {
                throw new IOException("Could not add message to inbox: " + e);
            }
        }

        void close() throws IOException {
            if (consumeChannel != null && consumeChannel.isOpen()) {
                consumeChannel.getConnection().close();
            }
        }
    }
}
