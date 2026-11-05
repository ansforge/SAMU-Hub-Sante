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

import static com.hubsante.hub.config.Constants.NEXSIS_HUBEX_PARTNER;
import static com.hubsante.hub.utils.MessageUtils.*;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.exception.UnroutableMessageException;
import com.hubsante.hub.service.ConversionHandler;
import com.hubsante.hub.service.MessageHandler;
import com.hubsante.hub.service.MessagePersistenceService;
import com.hubsante.hub.service.TopologyRegistry;
import com.hubsante.hub.utils.ConversionUtils;
import com.hubsante.hub.utils.EdxlUtils;
import com.hubsante.model.edxl.EdxlMessage;
import org.springframework.stereotype.Component;

/**
 * {@link RoutingStrategy} implementation for messages routed from Hub Santé to Fire (hubex)
 * partners.
 */
@Component
public class HubToFireStrategy extends HubSanteInternalStrategy implements RoutingStrategy {

    private final HubConfiguration hubConfig;

    public HubToFireStrategy(
            MessageHandler messageHandler,
            ConversionHandler conversionHandler,
            HubConfiguration hubConfig,
            MessagePersistenceService persistenceService) {
        super(messageHandler, conversionHandler, hubConfig, persistenceService);
        this.hubConfig = hubConfig;
    }

    // checkMessageContent is inherited as-is from HubSanteInternalStrategy: the sender is always
    // a health actor for this strategy too, so every check there (including the strict-equality
    // checkSenderConsistency, and the delivery-mode/distributionId-format checks) applies
    // identically here.

    @Override
    protected ConversionUtils.ConversionParametersDTO resolveConversionParameters(
            EdxlMessage edxlMessage) {
        String currentVhost = hubConfig.getVhost();
        String nexsisVhost = TopologyRegistry.getInstance().getVhostTarget(NEXSIS_HUBEX_PARTNER);

        if (ConversionUtils.isNexsisVhost(currentVhost)) {
            return null;
        }
        if (ConversionUtils.isCisuVhost(currentVhost)) {
            return ConversionUtils.ConversionParametersDTO.forVhostConversion(
                    edxlMessage,
                    currentVhost,
                    nexsisVhost,
                    ConversionUtils.ConversionType.CISU_VERSION_CONVERSION);
        }
        if (ConversionUtils.isHealthVhost(currentVhost)) {
            return ConversionUtils.ConversionParametersDTO.forVhostConversion(
                    edxlMessage,
                    currentVhost,
                    nexsisVhost,
                    ConversionUtils.ConversionType.CISU_TRANSCODING);
        }
        throw new UnroutableMessageException(
                "Cannot route message to Nexsis from vhost " + currentVhost,
                edxlMessage.getDistributionID(),
                getRecipientID(edxlMessage),
                EdxlUtils.getUseCaseFromMessage(edxlMessage.getFirstContentMessage()));
    }
}
