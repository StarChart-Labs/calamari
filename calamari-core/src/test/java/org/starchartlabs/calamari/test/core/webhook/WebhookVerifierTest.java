/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.webhook;

import org.starchartlabs.calamari.core.webhook.WebhookVerifier;
import org.testng.Assert;
import org.testng.annotations.Test;

public class WebhookVerifierTest {

    private static final String SECURE_TOKEN = "12345";

    private static final String PAYLOAD = "{ \"json\": \"json\" }";

    // Generated using an online HMAC-SHA1 tool to reduce chance of bug-masking
    private static final String EXPECTED_HMAC = "24510be1a28ed09a521e5929842ca47ebc05b414";

    private final WebhookVerifier webhookVerifier = new WebhookVerifier(() -> SECURE_TOKEN);

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullSecretLookup() throws Exception {
        new WebhookVerifier(null);
    }

    @Test
    public void isPayloadLegitimateNullSecurityKey() throws Exception {
        boolean result = webhookVerifier.isPayloadLegitimate(null, PAYLOAD);

        Assert.assertFalse(result);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void isPayloadLegitimateNullPayload() throws Exception {
        webhookVerifier.isPayloadLegitimate("sha1=" + EXPECTED_HMAC, null);
    }

    @Test
    public void isPayloadLegitimateUnmatchedSecurityKey() throws Exception {
        boolean result = webhookVerifier.isPayloadLegitimate("sha1=" + EXPECTED_HMAC + "nope", PAYLOAD);

        Assert.assertFalse(result);
    }

    @Test
    public void isPayloadLegitimateUnmatchedPayload() throws Exception {
        boolean result = webhookVerifier.isPayloadLegitimate("sha1=" + EXPECTED_HMAC, PAYLOAD + "{}");

        Assert.assertFalse(result);
    }

    @Test
    public void isPayloadLegitimate() throws Exception {
        boolean result = webhookVerifier.isPayloadLegitimate("sha1=" + EXPECTED_HMAC, PAYLOAD);

        Assert.assertTrue(result);
    }

}
