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

import static com.hubsante.hub.config.AmqpConfiguration.DISTRIBUTION_EXCHANGE;
import static com.hubsante.hub.config.Constants.FR_HEALTH_PREFIX;
import static com.hubsante.hub.utils.MessageUtils.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.AbstractHubException;
import com.hubsante.hub.exception.ConversionException;
import com.hubsante.hub.service.ConversionHandler;
import com.hubsante.hub.service.MessageHandler;
import com.hubsante.hub.service.MessagePersistenceService;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.hub.utils.MessagePersistencePolicy;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.List;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/** {@link RoutingStrategy} implementation for messages exchanged within the Hub Santé perimeter. */
@Component
public class HubSanteInternalStrategy implements RoutingStrategy {

    private final MessageHandler messageHandler;
    private final ConversionHandler conversionHandler;
    private final HubConfiguration hubConfig;
    private final MessagePersistenceService persistenceService;

    public HubSanteInternalStrategy(
            MessageHandler messageHandler,
            ConversionHandler conversionHandler,
            HubConfiguration hubConfig,
            MessagePersistenceService persistenceService) {
        this.messageHandler = messageHandler;
        this.conversionHandler = conversionHandler;
        this.hubConfig = hubConfig;
        this.persistenceService = persistenceService;
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
        // reject the message if the sender is not consistent with the routing key
        checkSenderConsistency(message, edxlMessage);
        // reject the message if the delivery mode is not PERSISTENT
        checkDeliveryModeIsPersistent(message, edxlMessage.getDistributionID());
        // reject the message if distributionId does not respect the format
        // (senderId_internalId)
        if (message.getMessageProperties().getReceivedRoutingKey().startsWith(FR_HEALTH_PREFIX)) {
            checkDistributionIDFormat(edxlMessage);
        }
    }

    @Override
    public List<MessageRoutingDTO> buildMessageRoutingDTO(Message message, EdxlMessage edxlMessage)
            throws AbstractHubException {
        ConversionUtils.ConversionParametersDTO conversionParameters =
                ConversionUtils.resolveConversionParameters(hubConfig, edxlMessage);

        if (conversionParameters != null) {
            return buildConvertedRoutingDTOs(message, edxlMessage, conversionParameters);
        }
        return List.of(buildDirectRoutingDTO(message, edxlMessage));
    }

    private List<MessageRoutingDTO> buildConvertedRoutingDTOs(
            Message message,
            EdxlMessage edxlMessage,
            ConversionUtils.ConversionParametersDTO conversionParameters) {
        if (conversionParameters.conversionType()
                == ConversionUtils.ConversionType.CISU_TRANSCODING) {
            String useCase = EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage());
            // Persist before conversion so the original message is saved even if conversion fails
            if (MessagePersistencePolicy.shouldPersist(hubConfig.getVhost(), useCase)) {
                persistenceService.persist(edxlMessage);
            }
        }

        List<String> convertedMessages;
        try {
            convertedMessages = conversionHandler.applyConversionRules(conversionParameters);
        } catch (JsonProcessingException e) {
            throw new ConversionException(
                    e.getMessage(),
                    edxlMessage.getDistributionID(),
                    getRecipientID(edxlMessage),
                    EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage()));
        }

        String destinationExchange =
                ConversionUtils.buildTransferExchangeName(
                        hubConfig.getVhost(), conversionParameters.targetVhost());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        return convertedMessages.stream()
                .map(
                        convertedMessage ->
                                new MessageRoutingDTO(
                                        messageHandler.forwardedStringMessage(
                                                convertedMessage, message),
                                        destinationExchange,
                                        routingKey))
                .toList();
    }

    private MessageRoutingDTO buildDirectRoutingDTO(Message message, EdxlMessage edxlMessage) {
        // Forward the message according to the recipient preferences. Conversion JSON <-> XML can
        // happen here
        Message forwardedMsg = messageHandler.forwardedMessage(edxlMessage, message);
        // Extract recipient queue name from the message (explicit address and distribution kind)
        String queueName = getRecipientQueueName(edxlMessage);
        return new MessageRoutingDTO(forwardedMsg, DISTRIBUTION_EXCHANGE, queueName);
    }
}
