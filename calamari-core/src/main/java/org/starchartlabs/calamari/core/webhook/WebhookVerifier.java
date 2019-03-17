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

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.starchartlabs.alloy.core.Suppliers;

/**
 * Provides behavior to verify the source of a webhook event payload
 *
 * @author romeara
 * @since 0.1.0
 */
public class WebhookVerifier {

    private final Supplier<HmacUtils> hmacLookup;

    /**
     * @param secureTokenLookup
     *            Supplier which provides an agreed-upon secret key for verifying GitHub payloads
     * @since 0.1.0
     */
    public WebhookVerifier(Supplier<String> secureTokenLookup) {
        Objects.requireNonNull(secureTokenLookup);

        this.hmacLookup = Suppliers.map(secureTokenLookup, token -> new HmacUtils(HmacAlgorithms.HMAC_SHA_1, token));
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
            String expected = "sha1=" + hmacLookup.get().hmacHex(payload);

            result = Objects.equals(securityKey, expected);
        }

        return result;
    }
}
