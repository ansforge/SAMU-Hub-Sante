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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hubsante.hub.utils.ConversionUtils.ConversionType;
import java.util.Collections;
import org.mockito.InOrder;

/**
 * Stubbing and verification helpers for {@link ConversionHandler#callConversionService}.
 *
 * <p>Lives in this package because that method is {@code protected}. Every helper applies the same
 * five-argument matcher tail, so tests only express what actually varies: what the service returns,
 * and which conversion type is expected.
 */
public final class ConversionStubs {

    private ConversionStubs() {}

    /** Returns the request body unchanged, as a single converted message. */
    public static void echoConversionService(ConversionHandler handler) {
        echoConversionService(handler, 1);
    }

    /** Returns the request body unchanged, repeated {@code copies} times. */
    public static void echoConversionService(ConversionHandler handler, int copies) {
        doAnswer(invocation -> Collections.nCopies(copies, invocation.getArgument(0).toString()))
                .when(handler)
                .callConversionService(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(ConversionType.class),
                        anyString());
    }

    /** Makes the conversion service throw {@code cause}. */
    public static void failConversionService(ConversionHandler handler, Throwable cause) {
        doThrow(cause)
                .when(handler)
                .callConversionService(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(ConversionType.class),
                        anyString());
    }

    /** Asserts exactly one conversion, of the expected type. */
    public static void verifyConversion(ConversionHandler handler, ConversionType expectedType) {
        verify(handler, times(1))
                .callConversionService(
                        anyString(), anyString(), anyString(), eq(expectedType), anyString());
    }

    /** Asserts exactly one conversion, of any type. */
    public static void verifyConversion(ConversionHandler handler) {
        verify(handler, times(1))
                .callConversionService(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(ConversionType.class),
                        anyString());
    }

    /** Asserts exactly one conversion, of any type, ordered against the other mocks. */
    public static void verifyConversion(InOrder inOrder, ConversionHandler handler) {
        inOrder.verify(handler, times(1))
                .callConversionService(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(ConversionType.class),
                        anyString());
    }

    /** Asserts no conversion at all. */
    public static void verifyNoConversion(ConversionHandler handler) {
        verify(handler, never())
                .callConversionService(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(ConversionType.class),
                        anyString());
    }
}
