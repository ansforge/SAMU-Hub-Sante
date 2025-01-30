/**
 * Copyright © 2023-2025 Agence du Numerique en Sante (ANS)
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
package com.hubsante.hub.utils;

import com.hubsante.model.builders.EDXL_DE_Builder;
import com.hubsante.model.edxl.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EdxlUtils {
    public static final String HUB_ID = "fr.health.hub";
    private static final long DEFAULT_HUB_MESSAGE_EXPIRATION = 1L;

    public static EdxlMessage edxlMessageFromHub(String recipientId, ContentMessage contentMessage) {
        return new EDXL_DE_Builder(UUID(),HUB_ID, recipientId)
                .dateTimeSentNowWithOffsetInDays(DEFAULT_HUB_MESSAGE_EXPIRATION)
                .distributionKind(DistributionKind.ERROR)
                .contentMessage(contentMessage)
                .build();
    }

    public static String getUseCaseFromMessage(ContentMessage contentMessage) {
        return contentMessage.getClass().getSimpleName();
    }
    private static String UUID() {
        return HUB_ID + "_" + UUID.randomUUID();
    }
}
