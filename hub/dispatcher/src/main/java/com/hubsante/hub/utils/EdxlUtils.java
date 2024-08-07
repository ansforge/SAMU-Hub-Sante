/**
 * Copyright © 2023-2024 Agence du Numerique en Sante (ANS)
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

import com.hubsante.modelsinterface.edxl.DistributionKind;
import com.hubsante.modelsinterface.interfaces.ContentMessageInterface;
import com.hubsante.modelsinterface.interfaces.EdxlHelperInterface;
import com.hubsante.modelsinterface.interfaces.EdxlMessageInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EdxlUtils {
    public static final String HUB_ID = "fr.health.hub";
    private static final long DEFAULT_HUB_MESSAGE_EXPIRATION = 1L;

    @Autowired
    private static EdxlHelperInterface edxlDeHelper;

    public static EdxlMessageInterface edxlMessageFromHub(String recipientId, ContentMessageInterface contentMessage) {
        return edxlDeHelper.buildEdxlMessage(UUID(),
                HUB_ID,
                recipientId,
                DEFAULT_HUB_MESSAGE_EXPIRATION,
                DistributionKind.ERROR,
                contentMessage);
    }

    private static String UUID() {
        return HUB_ID + "_" + UUID.randomUUID();
    }
}
