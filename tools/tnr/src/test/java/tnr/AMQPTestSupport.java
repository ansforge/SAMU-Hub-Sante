package tnr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static tnr.Constants.TLS_PROTOCOL_VERSION;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.rabbitmq.client.Delivery;

import io.github.cdimascio.dotenv.Dotenv;
import tnr.dto.MessageDTO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AMQPTestSupport {

    private static final Logger logger = LoggerFactory.getLogger(AMQPTestSupport.class);

    protected final Map<String, Collection<String>> config = new HashMap<>(Map.of(
            "15-15_v1.5", List.of("fr.health.tnr.samu1-v1", "fr.health.tnr.samu2-v1"),
            "15-15_v2.0", List.of("fr.health.tnr.samu1-v2", "fr.health.tnr.samu2-v2"),
            "15-15_v2.1", List.of("fr.health.tnr.samu1-v3", "fr.health.tnr.samu2-v3"),
            "15-nexsis_v1.9", List.of("fr.health.fire", "fr.health.tnr.samu2-v3")
    ));

    protected static final String BASE_URL = "https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/refs/tags";

    protected Map<String, Producer> producers = new HashMap<>();
    protected Collection<TestConsumer> consumers = new ArrayList<>();

    protected static final int RECEIVE_TIMEOUT_SECS = 10;

    protected Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    HttpClientWithCache httpClient = new HttpClientWithCache(dotenv.get("GITHUB_TOKEN"));

    // latency metrics across all sends/receives in this test class
    protected final TestMetrics metrics = new TestMetrics();

    // global message collector
    protected final MessageCollector inbox = new MessageCollector(metrics);

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

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
                logger.warn("Could not tear down consumer {}: {}", consumer.getClientId(), e.getMessage());
            }
        });
        producers.values().forEach(producer -> {
            try {
                producer.close();
            } catch (Exception e) {
                logger.warn("Could not tear down producer {}: {}", producer.getVhost(), e.getMessage());
            }
        });
        metrics.logSummary(logger);
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

    protected void send(String vhost, String routingKey, String message) throws Exception {
        Producer producer = getProducer(vhost);
        String distributionId = extractDistributionId(message);
        if (distributionId != null) {
            metrics.recordSent(distributionId, vhost);
        }
        producer.publish(routingKey, message.getBytes());
    }

    private String extractDistributionId(String message) {
        try {
            JsonNode node = jsonMapper.readTree(message);
            JsonNode d = node.get("distributionID");
            if (d != null) return d.asText();
        } catch (Exception ignored) { }
        try {
            JsonNode node = xmlMapper.readTree(message.getBytes(StandardCharsets.UTF_8));
            JsonNode d = node.get("distributionID");
            if (d != null) return d.asText();
        } catch (Exception ignored) { }
        return null;
    }

    protected void sendMessage(String vhost, String routingKey, String message) throws Exception {
        logger.info("[{}] Sending message on routingKey {}", vhost, routingKey);
        send(vhost, routingKey, message);
    }

    String sendAck(String vhost, String routingKey, String recipientId, String referencedDistributionId) throws Exception {
        String ackDistributionId = Utils.generateDistributionId(routingKey);
        String refMessage = new AckBuilder().buildAck(routingKey, recipientId, ackDistributionId, referencedDistributionId);
        logger.info("[{}] Sending ack on routingKey {} to {}, referencing {}", vhost, routingKey, recipientId, referencedDistributionId);
        send(vhost, routingKey, refMessage);
        return ackDistributionId;
    }

    private Producer getProducer(String vhost) {
        return producers.get(vhost);
    }

    private Producer createProducer(String host, int port, String vhost, String exchange, TLSConf tlsConf) throws Exception {
        logger.info("Creating producer on vhost {}", vhost);
        logger.debug("host:{}, vhost:{}, port:{}, exchange: {}", host, vhost, port, exchange);
        Producer p = new Producer(host, port, vhost, exchange);
        logger.info("[{}] Connecting producer to rabbitmq.", vhost);
        p.connect(tlsConf);
        return p;
    }

    private TestConsumer createConsumer(String host, int port, String vhost, String exchange,
            String clientId, String queueName, MessageCollector inbox, TLSConf tlsConf) throws Exception {
        logger.info("Creating consumer on queue {}", queueName);
        logger.debug("host:{}, vhost:{}, port:{}, exchange:{}, clientId:{}, queueName:{}", host, vhost, port, exchange, clientId, queueName);
        TestConsumer c = new TestConsumer(host, port, vhost, exchange, queueName, clientId, inbox);
        logger.info("[queue {}] Connecting consumer to rabbitmq.", queueName);
        c.connect(tlsConf);
        return c;
    }


    static class MessageCollector {

        private final ConcurrentHashMap<String, CompletableFuture<MessageDTO>> pending = new ConcurrentHashMap<>();
        private final List<MessageDTO> buffer = new ArrayList<>();
        private final ReentrantLock lock = new ReentrantLock();
        ObjectMapper mapper = new ObjectMapper();
        private final TestMetrics metrics;

        MessageCollector(TestMetrics metrics) {
            this.metrics = metrics;
        }

        void add(MessageDTO delivery) {
            if (metrics != null) {
                metrics.recordReceived(delivery.getDistributionId(), delivery.getVhost());
            }
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
                logger.error("Could not receive message that matched " + distributionId);
                throw e;
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

    static class TestMetrics {

        private static class SentInfo {
            final long sendNanos;
            SentInfo(long sendNanos) {
                this.sendNanos = sendNanos;
            }
        }

        private static class Stats {
            long count = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            long sumNanos = 0;
            void add(long ns) {
                count++;
                sumNanos += ns;
                if (ns < min) min = ns;
                if (ns > max) max = ns;
            }
        }

        private final ConcurrentHashMap<String, SentInfo> sent = new ConcurrentHashMap<>();
        private final Map<String, Stats> perReceiveVhost = new ConcurrentHashMap<>();
        private final java.util.Set<String> sendVhosts = ConcurrentHashMap.newKeySet();
        private final Stats global = new Stats();
        private final Object globalLock = new Object();

        void recordSent(String distributionId, String vhost) {
            sent.put(distributionId, new SentInfo(System.nanoTime()));
            sendVhosts.add(vhost);
        }

        void recordReceived(String distributionId, String receiveVhost) {
            SentInfo info = sent.remove(distributionId);
            if (info == null) return;
            long latency = System.nanoTime() - info.sendNanos;
            synchronized (globalLock) {
                global.add(latency);
            }
            perReceiveVhost.compute(receiveVhost, (k, v) -> {
                Stats s = (v == null) ? new Stats() : v;
                synchronized (s) { s.add(latency); }
                return s;
            });
        }

        void logSummary(Logger logger) {
            synchronized (globalLock) {
                if (global.count == 0) {
                    logger.info("=== TNR latency metrics === no samples collected.");
                    return;
                }
                double avgMs = (global.sumNanos / (double) global.count) / 1_000_000.0;
                double minMs = global.min / 1_000_000.0;
                double maxMs = global.max / 1_000_000.0;
                logger.info("=== TNR latency metrics ===");
                logger.info("Samples: {}", global.count);
                logger.info("Avg latency: {} ms", String.format("%.2f", avgMs));
                logger.info("Min latency: {} ms", String.format("%.2f", minMs));
                logger.info("Max latency: {} ms", String.format("%.2f", maxMs));
                logger.info("Send vhosts: {}", sendVhosts);
                logger.info("Receive vhosts:");
                perReceiveVhost.forEach((vhost, s) -> {
                    synchronized (s) {
                        double a = (s.sumNanos / (double) s.count) / 1_000_000.0;
                        logger.info("  - {} : count={}, avg={} ms, min={} ms, max={} ms",
                                vhost, s.count,
                                String.format("%.2f", a),
                                String.format("%.2f", s.min / 1_000_000.0),
                                String.format("%.2f", s.max / 1_000_000.0));
                    }
                });
                if (!sent.isEmpty()) {
                    logger.warn("Unreceived messages: {}", sent.keySet());
                }
            }
        }
    }
}
