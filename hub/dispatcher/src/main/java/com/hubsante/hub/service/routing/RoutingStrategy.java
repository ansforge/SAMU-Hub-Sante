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

import com.hubsante.hub.exception.AbstractHubException;
import com.hubsante.model.edxl.EdxlMessage;
import java.util.List;
import org.springframework.amqp.core.Message;

/**
 * Defines how a message received by the dispatcher is checked and routed to its recipient(s). Each
 * hub perimeter (internal Hub Santé, hubex partners, ...) can implement its own strategy.
 */
public interface RoutingStrategy {

    /**
     * Checks that the message is allowed to be routed, given its AMQP envelope and its EDXL content.
     * Throws an {@link AbstractHubException} if any of the checks fails.
     */
    void checkMessageContent(Message message, EdxlMessage edxlMessage) throws AbstractHubException;

    /**
     * Builds the list of AMQP messages to publish in order to route the message to its recipient,
     * along with the exchange and routing key each one should be published to.
     */
    List<MessageRoutingDTO> buildMessageRoutingDTO(Message message, EdxlMessage edxlMessage)
            throws AbstractHubException;
}
