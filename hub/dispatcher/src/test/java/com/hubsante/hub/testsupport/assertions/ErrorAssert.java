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
package com.hubsante.hub.testsupport.assertions;

import com.hubsante.model.report.Error;
import com.hubsante.model.report.ErrorCode;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class ErrorAssert extends AbstractAssert<ErrorAssert, Error> {

    ErrorAssert(Error actual) {
        super(actual, ErrorAssert.class);
    }

    public ErrorAssert hasCode(ErrorCode expected) {
        isNotNull();
        Assertions.assertThat(actual.getErrorCode()).as("error code").isEqualTo(expected);
        return this;
    }

    public ErrorAssert references(String expectedDistributionId) {
        isNotNull();
        Assertions.assertThat(actual.getReferencedDistributionID())
                .as("referenced distributionID of the %s error", actual.getErrorCode())
                .isEqualTo(expectedDistributionId);
        return this;
    }

    public ErrorAssert hasCauseContaining(String... fragments) {
        isNotNull();
        Assertions.assertThat(actual.getErrorCause())
                .as("error cause of the %s error", actual.getErrorCode())
                .contains(fragments);
        return this;
    }
}
