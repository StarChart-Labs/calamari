/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.starchartlabs.calamari.core.ResponseConditions;
import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;
import org.testng.annotations.Test;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class ResponseConditionsTest {

    private static final String RATE_LIMIT_MAXIMUM_HEADER = "X-RateLimit-Limit";

    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    @Test(expectedExceptions = NullPointerException.class)
    public void checkRateLimitResponseNullResponse() throws Exception {
        ResponseConditions.checkRateLimit(null);
    }

    @Test
    public void checkRateLimitResponseNotExceeded() throws Exception {
        Response response = getResponseBuilder()
                .code(100)
                .header(RATE_LIMIT_MAXIMUM_HEADER, "1000")
                .header(RATE_LIMIT_RESET_HEADER, "5")
                .header(RATE_LIMIT_REMAINING_HEADER, "500")
                .build();

        ResponseConditions.checkRateLimit(response);
    }

    @Test
    public void checkRateLimitResponseNotExceededForbidden() throws Exception {
        Response response = getResponseBuilder()
                .code(403)
                .header(RATE_LIMIT_MAXIMUM_HEADER, "1000")
                .header(RATE_LIMIT_RESET_HEADER, "5")
                .header(RATE_LIMIT_REMAINING_HEADER, "500")
                .build();

        ResponseConditions.checkRateLimit(response);
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void checkRateLimitResponse() throws Exception {
        Response response = getResponseBuilder()
                .code(403)
                .header(RATE_LIMIT_MAXIMUM_HEADER, "1000")
                .header(RATE_LIMIT_RESET_HEADER, "5")
                .header(RATE_LIMIT_REMAINING_HEADER, "0")
                .build();

        ResponseConditions.checkRateLimit(response);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void checkRateLimitNullResponse() throws Exception {
        ResponseConditions.checkRateLimit((String) null, a -> 100, (a, b) -> Collections.emptyList());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void checkRateLimitNullCodeLookup() throws Exception {
        ResponseConditions.checkRateLimit("a", null, (a, b) -> Collections.emptyList());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void checkRateLimitNullHeaderLookup() throws Exception {
        ResponseConditions.checkRateLimit("a", a -> 100, null);
    }

    @Test
    public void checkRateLimitNotExceeded() throws Exception {
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put(RATE_LIMIT_MAXIMUM_HEADER, Collections.singleton("1000"));
        headers.put(RATE_LIMIT_RESET_HEADER, Collections.singleton("5"));
        headers.put(RATE_LIMIT_REMAINING_HEADER, Collections.singleton("500"));

        ResponseConditions.checkRateLimit("a", a -> 100, (a, b) -> headers.get(b));
    }

    @Test
    public void checkRateLimitNotExceededForbidden() throws Exception {
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put(RATE_LIMIT_MAXIMUM_HEADER, Collections.singleton("1000"));
        headers.put(RATE_LIMIT_RESET_HEADER, Collections.singleton("5"));
        headers.put(RATE_LIMIT_REMAINING_HEADER, Collections.singleton("500"));

        ResponseConditions.checkRateLimit("a", a -> 403, (a, b) -> headers.get(b));
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void checkRateLimit() throws Exception {
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put(RATE_LIMIT_MAXIMUM_HEADER, Collections.singleton("1000"));
        headers.put(RATE_LIMIT_RESET_HEADER, Collections.singleton("5"));
        headers.put(RATE_LIMIT_REMAINING_HEADER, Collections.singleton("0"));

        ResponseConditions.checkRateLimit("a", a -> 403, (a, b) -> headers.get(b));
    }

    private Response.Builder getResponseBuilder() {
        return new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .message("message");
    }

}
