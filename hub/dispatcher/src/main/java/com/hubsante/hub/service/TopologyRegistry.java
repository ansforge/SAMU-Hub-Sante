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
package com.hubsante.hub.service;

import com.hubsante.hub.config.Constants;
import com.hubsante.hub.exception.ClientConfigurationException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TopologyRegistry {

    // Spring creates exactly one instance of this bean; static utility classes that can't
    // take a constructor-injected dependency (MessagePersistencePolicy, ConversionUtils, ...)
    // read it through this self-registered reference instead of threading it through every
    // method signature.
    private static TopologyRegistry instance;

    private final Map<String, String> majorModelVersionPerVhost;
    private final Map<String, String> vhostTargetPerHubexPartner;

    @SuppressWarnings("unchecked")
    public TopologyRegistry(@Value("${client.configuration.file}") Resource resource) {
        YamlMapFactoryBean mapFactoryBean = new YamlMapFactoryBean();
        mapFactoryBean.setResources(resource);
        Map<String, Object> root = mapFactoryBean.getObject();

        majorModelVersionPerVhost = (Map<String, String>) root.get("majorModelVersionPerVhost");
        vhostTargetPerHubexPartner = (Map<String, String>) root.get("vhostTargetPerHubexPartner");

        if (majorModelVersionPerVhost == null || vhostTargetPerHubexPartner == null) {
            throw new ClientConfigurationException(
                    "Missing 'majorModelVersionPerVhost' or 'vhostTargetPerHubexPartner' in topology configuration file");
        }
        if (vhostTargetPerHubexPartner.get(Constants.NEXSIS_HUBEX_PARTNER) == null) {
            throw new ClientConfigurationException(
                    "Missing '"
                            + Constants.NEXSIS_HUBEX_PARTNER
                            + "' entry in 'vhostTargetPerHubexPartner' topology configuration");
        }
        instance = this;
    }

    public static TopologyRegistry getInstance() {
        return instance;
    }

    public String getMajorModelVersion(String vhost) {
        return majorModelVersionPerVhost.get(vhost);
    }

    public String getVhostTarget(String hubexPartner) {
        return vhostTargetPerHubexPartner.get(hubexPartner);
    }
}
