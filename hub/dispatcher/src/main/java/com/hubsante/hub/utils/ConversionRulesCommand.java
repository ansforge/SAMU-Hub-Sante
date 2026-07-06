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
package com.hubsante.hub.utils;

import com.hubsante.hub.config.HubConfiguration;
import com.hubsante.hub.service.TopologyRegistry;
import com.hubsante.model.edxl.EdxlMessage;

public class ConversionRulesCommand {
    EdxlMessage edxlMessage;
    String sourceVHost;
    String targetVHost;
    String sourceModelVersion;
    String targetModelVersion;
    Boolean isCisuConversion;

    public ConversionRulesCommand(EdxlMessage edxlMessage, HubConfiguration hubConfig) {
        this.edxlMessage = edxlMessage;
        this.sourceVHost = hubConfig.getVhost();

        String[] targetVhosts = ConversionUtils.getTargetVHosts(hubConfig, edxlMessage);
        this.targetVHost = targetVhosts[targetVhosts.length - 1];

        this.isCisuConversion = ConversionUtils.requiresCisuConversion(hubConfig, edxlMessage);
        this.sourceModelVersion = getVHostMatchingModelVersion(sourceVHost);
        this.targetModelVersion = getVHostMatchingModelVersion(targetVHost);
    }

    public String getTargetVHost() {
        return targetVHost;
    }

    public Boolean getCisuConversion() {
        return isCisuConversion;
    }

    public String getSourceVHost() {
        return sourceVHost;
    }

    public String getSourceModelVersion() {
        return sourceModelVersion;
    }

    public String getTargetModelVersion() {
        return targetModelVersion;
    }

    public EdxlMessage getEdxlMessage() {
        return edxlMessage;
    }

    public String getVHostMatchingModelVersion(String vHost) {
        String modelVersion = TopologyRegistry.getInstance().getMajorModelVersion(vHost);
        if (modelVersion == null) {
            throw new IllegalArgumentException(
                    "There is no model version associated with the host " + vHost);
        }
        return modelVersion;
    }
}
