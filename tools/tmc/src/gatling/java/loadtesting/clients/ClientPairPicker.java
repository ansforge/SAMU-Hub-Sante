package loadtesting.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class ClientPairPicker {
    private static final Logger log = LoggerFactory.getLogger(ClientPairPicker.class);

    private final List<ClientPair> pairs;

    public ClientPairPicker(String scenario, ClientPool senders, ClientPool recipients) {
        this.pairs = senders.clientIds().stream()
                .flatMap(sender -> recipients.clientIds().stream()
                        .filter(recipient -> !recipient.equals(sender))
                        .map(recipient -> new ClientPair(sender, recipient)))
                .toList();

        if (pairs.isEmpty()) {
            throw new ClientConfigurationException(String.format(
                    "No sender/recipient pair available for %s: senders (%s) = %s, recipients (%s) = %s",
                    scenario, senders.description(), senders.clientIds(),
                    recipients.description(), recipients.clientIds()));
        }

        Set<Set<String>> unorderedPairs = pairs.stream()
                .map(pair -> Set.of(pair.senderId(), pair.recipientId()))
                .collect(Collectors.toSet());
        if (unorderedPairs.size() == 1) {
            log.warn("Only one sender/recipient pair available for {}: {}, add clients to the configuration for more variety",
                    scenario, unorderedPairs.iterator().next());
        }
    }

    public List<ClientPair> pairs() {
        return pairs;
    }

    public ClientPair pick() {
        return pairs.get(ThreadLocalRandom.current().nextInt(pairs.size()));
    }
}
