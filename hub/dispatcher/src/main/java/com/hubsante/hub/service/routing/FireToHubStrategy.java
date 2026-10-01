/**
 * Copyright © 2023-2026 Agence du Numerique en Sante (ANS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubsante.hub.service.routing;

import static com.hubsante.hub.utils.MessageUtils.*;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.AbstractHubException;
import com.hubsante.hub.exception.SenderInconsistencyException;
import com.hubsante.hub.service.ConversionHandler;
import com.hubsante.hub.service.MessageHandler;
import com.hubsante.hub.service.MessagePersistenceService;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.edxl.EdxlMessage;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * {@link RoutingStrategy} implementation for messages routed from Fire (hubex) partners to Hub
 * Santé.
 */
@Component
public class FireToHubStrategy extends HubSanteInternalStrategy implements RoutingStrategy {

    private final MessageHandler messageHandler;
    private final HubConfiguration hubConfig;

    public FireToHubStrategy(
            MessageHandler messageHandler,
            ConversionHandler conversionHandler,
            HubConfiguration hubConfig,
            MessagePersistenceService persistenceService) {
        super(messageHandler, conversionHandler, hubConfig, persistenceService);
        this.messageHandler = messageHandler;
        this.hubConfig = hubConfig;
    }

    @Override
    public void checkMessageContent(Message message, EdxlMessage edxlMessage)
            throws AbstractHubException {
        // check message type is allowed on the current vhost
        checkMessageClassNameSupported(edxlMessage, hubConfig);
        // check message is allowed for its recipient
        messageHandler.inhibitMessageIfNeeded(edxlMessage);
        // reject the message if no health actor is involved (as sender or recipient)
        checkHealthActorIsInvolved(edxlMessage);
        // hubex partners publish through a shared technical routing key that will not always
        // match the functional senderId, so we only require both to share the same domain prefix
        checkSenderConsistency(message, edxlMessage);
    }

    private void checkSenderConsistency(Message message, EdxlMessage edxlMessage) {
        String receivedRoutingKey = getSenderFromRoutingKey(message);
        String senderId = edxlMessage.getSenderID();
        String expectedPrefix = extractDomainPrefix(receivedRoutingKey);

        if (!senderId.startsWith(expectedPrefix)) {
            String recipientId = getRecipientID(edxlMessage);
            String messageType =
                    EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
            String errorCause =
                    "Sender inconsistency for message "
                            + edxlMessage.getDistributionID()
                            + " : message sender is "
                            + senderId
                            + " but received routing key is "
                            + receivedRoutingKey
                            + ", expected sender to be prefixed with "
                            + expectedPrefix;
            throw new SenderInconsistencyException(
                    errorCause, edxlMessage.getDistributionID(), recipientId, messageType);
        }
    }
}
