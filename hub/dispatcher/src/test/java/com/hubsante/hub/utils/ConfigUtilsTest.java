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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ConfigUtils")
class ConfigUtilsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "15-15_v2.1,        15-15-v2.1",
        "15-nexsis_vactive, 15-nexsis-vactive",
        "15-15_v1.5_extra,  15-15-v1.5-extra",
        "no-underscore,     no-underscore",
        "'',                ''",
    })
    @DisplayName("should replace underscores, which Prometheus label values cannot carry")
    void shouldSanitizeVhostForProm(String vhost, String expected) {
        assertThat(ConfigUtils.sanitizeVhostForProm(vhost)).isEqualTo(expected);
    }
}
