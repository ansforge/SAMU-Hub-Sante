package loadtesting.simulations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubsante.model.Utils;
import com.hubsante.model.edxl.*;
import com.hubsante.model.geolocation.GeoPositionsUpdate;
import com.hubsante.model.geolocation.GeoPositionsUpdateWrapper;
import com.hubsante.model.rcde.DistributionElement;
import com.hubsante.model.rcde.Recipient;
import com.hubsante.model.rcde.Sender;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import loadtesting.AmqpConnectionFactory;
import loadtesting.SimulationUtils;
import org.galaxio.gatling.amqp.javaapi.protocol.AmqpProtocolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static loadtesting.Constants.JSON_CONTENT_TYPE;
import static org.galaxio.gatling.amqp.javaapi.AmqpDsl.amqp;

public class SamuGpsSimulation extends Simulation {
    private final static int defaultDuration = 600; // 10 minutes
    private final static int defaultUserCount = 160;
    private final static String vhost = "15-gps_v1.3";
    private final static String senderId = "fr.health.test.samuA";
    private final static String recipientId = "fr.health.test.samuC";
    private final static String clientIdPrefix = "fr.health.";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper jsonMapper = Utils.getJsonMapper();

    private String buildMessageStringAtCurrentTime(GeoPositionsUpdate content) {
        try {
            OffsetDateTime sentAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            OffsetDateTime expiresAt = sentAt.plusDays(1);

            String distributionId = String.format("%s_%s", senderId, UUID.randomUUID());

            String sanitizedSenderId = senderId.substring(clientIdPrefix.length());
            String sanitizedRecipientId = recipientId.substring(clientIdPrefix.length());

            Sender sender = new Sender().name(sanitizedSenderId).URI(String.format("hubex:%s", sanitizedSenderId));
            Recipient recipient = new Recipient().name(sanitizedRecipientId).URI(String.format("hubex:%s", sanitizedRecipientId));
            List<Recipient> recipients = new ArrayList<>();
            recipients.add(recipient);

            GeoPositionsUpdateWrapper contentMessageWrapper = new GeoPositionsUpdateWrapper();
            contentMessageWrapper.setGeoPositionsUpdate(content);
            contentMessageWrapper.setRecipient(recipients);
            contentMessageWrapper.setSender(sender);
            contentMessageWrapper.setMessageId(distributionId);
            contentMessageWrapper.setSentAt(sentAt);
            contentMessageWrapper.setKind(DistributionElement.KindEnum.REPORT);
            contentMessageWrapper.setStatus(DistributionElement.StatusEnum.ACTUAL);

            ExplicitAddress recipientAddress = new ExplicitAddress("hubex", recipientId);
            Descriptor descriptor = new Descriptor("fr-FR", recipientAddress);

            EdxlMessage message = new EdxlMessage(distributionId, senderId, sentAt, expiresAt,
                    DistributionStatus.ACTUAL, DistributionKind.REPORT, descriptor, contentMessageWrapper);

            return jsonMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error occurred when building message", e);
            return "{}";
        }
    }

    {
        try {
            AmqpConnectionFactory connectionFactory = new AmqpConnectionFactory();
            AmqpProtocolBuilder gpsConnection = connectionFactory.buildAmqpProtocolBuilder(vhost);

            String fileContent = SimulationUtils.loadSampleFile("geo-position.json");
            GeoPositionsUpdate contentMessage = jsonMapper.readValue(fileContent, GeoPositionsUpdate.class);

            Iterator<Map<String, Object>> messageFeeder = Stream.generate((Supplier<Map<String, Object>>) () ->
                    Collections.singletonMap("message", buildMessageStringAtCurrentTime(contentMessage))).iterator();

            ScenarioBuilder gpsScenario = scenario("GEO-POS")
                    .feed(
                            messageFeeder
                    )
                    .exec(
                            amqp("publish message as client " + senderId)
                                    .publish()
                                    .topicExchange("hubsante", senderId)
                                    .textMessage("#{message}")
                                    .contentType(JSON_CONTENT_TYPE)
                    );

            int duration = SimulationUtils.getNumericEnvVar("SCENARIO_DURATION", defaultDuration);
            int userCount = SimulationUtils.getNumericEnvVar("SAMU_GPS_SCENARIO_USER_COUNT", defaultUserCount);

            setUp(
                    gpsScenario.injectOpen(
                            constantUsersPerSec(userCount).during(duration)
                    ).protocols(gpsConnection)
            ).maxDuration(duration * 2L);
        } catch (Exception e) {
            log.error("Unexpected error during load test", e);
        }
    }
}