/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.webhook;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.codec.digest.HmacUtils;

/**
 * Provides behavior to verify the source of a webhook event payload
 *
 * @author romeara
 * @since 0.1.0
 */
public class WebhookVerifier {

    private final Supplier<String> secureTokenLookup;

    /**
     * @param secureTokenLookup
     *            Supplier which provides an agreed-upon secret key for verifying GitHub payloads
     * @since 0.1.0
     */
    public WebhookVerifier(Supplier<String> secureTokenLookup) {
        this.secureTokenLookup = Objects.requireNonNull(secureTokenLookup);
    }

    /**
     * Determines if a payload was sent by GitHub via an agreed-upon token and hashing strategy
     *
     * @param securityKey
     *            The header provided with the event with the hash to verify
     * @param payload
     *            The payload that was sent with the header
     * @return True if the payload was verified as sent by GitHub, false otherwise
     * @since 0.1.0
     */
    public boolean isPayloadLegitimate(@Nullable String securityKey, String payload) {
        Objects.requireNonNull(payload);

        boolean result = false;

        if (securityKey != null) {
            String secureToken = secureTokenLookup.get();
            String expected = "sha1=" + HmacUtils.hmacSha1Hex(secureToken, payload);

            result = Objects.equals(securityKey, expected);
        }

        return result;
    }
}
