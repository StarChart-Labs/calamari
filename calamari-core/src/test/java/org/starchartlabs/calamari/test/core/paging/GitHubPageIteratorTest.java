/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.paging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.starchartlabs.alloy.core.collections.PageIterator;
import org.starchartlabs.calamari.core.MediaTypes;
import org.starchartlabs.calamari.core.exception.GitHubResponseException;
import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;
import org.starchartlabs.calamari.core.paging.GitHubPageIterator;
import org.starchartlabs.calamari.test.LinkHeaderTestSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.gson.JsonElement;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class GitHubPageIteratorTest {

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullUrl() throws Exception {
        new GitHubPageIterator<String>(null, () -> "header", "userAgent", a -> Collections.singletonList(a),
                "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullAuthorizationHeader() throws Exception {
        new GitHubPageIterator<String>("url", null, "userAgent", a -> Collections.singletonList(a), "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullUserAgent() throws Exception {
        new GitHubPageIterator<String>("url", () -> "header", null, a -> Collections.singletonList(a),
                "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullJsonDeserializer() throws Exception {
        new GitHubPageIterator<String>("url", () -> "header", "userAgent", null, "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullMediaType() throws Exception {
        new GitHubPageIterator<String>("url", () -> "header", "userAgent", a -> Collections.singletonList(a),
                null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void gsonNullUrl() throws Exception {
        GitHubPageIterator.gson(null, () -> "header", "userAgent", "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void gsonNullAuthorizationHeader() throws Exception {
        GitHubPageIterator.gson("url", null, "userAgent", "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void gsonNullUserAgent() throws Exception {
        GitHubPageIterator.gson("url", () -> "header", null, "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void gsonNullMediaType() throws Exception {
        GitHubPageIterator.gson("url", () -> "header", "userAgent", null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void mapNullMapper() throws Exception {
        GitHubPageIterator.gson("url", () -> "header", "userAgent")
        .map(null);
    }

    @Test(expectedExceptions = GitHubResponseException.class)
    public void nextErrorResponse() throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(412);

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String authorizationHeader = "header";
            String userAgent = "userAgent";
            String path = "/api/endpoint";

            String url = server.url(path).toString();

            GitHubPageIterator<String> iterator = new GitHubPageIterator<>(url, () -> authorizationHeader, userAgent,
                    a -> Collections.singletonList(a));

            try {
                iterator.next();
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request.getPath(), path);
            }
        }
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void nextRequestLimitExceeded() throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(403)
                .addHeader(RATE_LIMIT_REMAINING_HEADER, "0");

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String authorizationHeader = "header";
            String userAgent = "userAgent";
            String path = "/api/endpoint";

            String url = server.url(path).toString();

            GitHubPageIterator<String> iterator = new GitHubPageIterator<>(url, () -> authorizationHeader, userAgent,
                    a -> Collections.singletonList(a));

            try {
                iterator.next();
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request.getPath(), path);
            }
        }
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void nextNoMoreElements() throws Exception {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                .setBody("[]");

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String authorizationHeader = "header";
            String userAgent = "userAgent";
            String path = "/api/endpoint";

            String url = server.url(path).toString();

            GitHubPageIterator<?> iterator = GitHubPageIterator.gson(url, () -> authorizationHeader, userAgent);

            // First should pass with original response
            try {
                iterator.next();
            } catch (RuntimeException e) {
                Assert.fail("First call should succeed, elements still remain", e);
            }

            try {
                iterator.next();
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request.getPath(), path);
            }
        }
    }

    @Test
    public void nextSinglePage() throws Exception {
        // Single page has content and no links
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                .setBody(getResponseContent("1", "2"));

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String authorizationHeader = "header";
            String userAgent = "userAgent";
            String path = "/api/endpoint";

            String url = server.url(path).toString();

            GitHubPageIterator<String> iterator = GitHubPageIterator.gson(url, () -> authorizationHeader, userAgent)
                    .map(JsonElement::getAsString);

            // First should pass with original response
            try {
                Collection<String> result = iterator.next();

                List<String> orderedResult = new ArrayList<>(result);

                Assert.assertNotNull(result);
                Assert.assertEquals(result.size(), 2);
                Assert.assertEquals(orderedResult.get(0), "1");
                Assert.assertEquals(orderedResult.get(1), "2");

                Assert.assertFalse(iterator.hasNext());
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request.getPath(), path);
            }
        }
    }

    @Test
    public void nextMultiplePages() throws Exception {
        String path = "/api/endpoint";

        try (MockWebServer server = new MockWebServer()) {
            server.start();

            Collection<String> firstPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 1, 3, 2);
            Collection<String> secondPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 2, 3, 2);
            Collection<String> thirdPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 3, 3, 2);

            MockResponse response1 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("1", "2"));

            firstPageLinks.forEach(link -> response1.addHeader("Link", link));

            MockResponse response2 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("3", "4"));

            secondPageLinks.forEach(link -> response2.addHeader("Link", link));

            MockResponse response3 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("5", "6"));

            thirdPageLinks.forEach(link -> response3.addHeader("Link", link));

            server.enqueue(response1);
            server.enqueue(response2);
            server.enqueue(response3);

            String authorizationHeader = "header";
            String userAgent = "userAgent";

            String url = server.url(path).toString();

            GitHubPageIterator<String> iterator = GitHubPageIterator.gson(url, () -> authorizationHeader, userAgent)
                    .map(JsonElement::getAsString);

            // First should pass with original response
            try {
                Collection<String> result = iterator.next();
                List<String> orderedResult = new ArrayList<>(result);

                Assert.assertNotNull(result);
                Assert.assertEquals(result.size(), 2);
                Assert.assertEquals(orderedResult.get(0), "1");
                Assert.assertEquals(orderedResult.get(1), "2");

                Assert.assertTrue(iterator.hasNext());

                result = iterator.next();
                orderedResult = new ArrayList<>(result);

                Assert.assertNotNull(result);
                Assert.assertEquals(result.size(), 2);
                Assert.assertEquals(orderedResult.get(0), "3");
                Assert.assertEquals(orderedResult.get(1), "4");

                Assert.assertTrue(iterator.hasNext());

                result = iterator.next();
                orderedResult = new ArrayList<>(result);

                Assert.assertNotNull(result);
                Assert.assertEquals(result.size(), 2);
                Assert.assertEquals(orderedResult.get(0), "5");
                Assert.assertEquals(orderedResult.get(1), "6");

                Assert.assertFalse(iterator.hasNext());
            } finally {
                Assert.assertEquals(server.getRequestCount(), 3);
                RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request1.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request1.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request1.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request1.getPath(), path);

                RecordedRequest request2 = server.takeRequest(2, TimeUnit.SECONDS);

                Assert.assertEquals(request2.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request2.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request2.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request2.getPath(), path + "?page=2&per_page=2");

                RecordedRequest request3 = server.takeRequest(3, TimeUnit.SECONDS);

                Assert.assertEquals(request3.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request3.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request3.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request3.getPath(), path + "?page=3&per_page=2");
            }
        }
    }

    @Test
    public void trySplit() throws Exception {
        PageIterator<?> result = GitHubPageIterator.gson("url", () -> "header", "userAgent").trySplit();

        // Currently, the GitHubPageIterator implementation does not support splitting
        Assert.assertNull(result);
    }

    @Test
    public void estimateSizeNoEstimate() throws Exception {
        // Before paging is started, the implementation cannot meaningfully estimate the remaining elements
        long result = GitHubPageIterator.gson("url", () -> "header", "userAgent").estimateSize();

        Assert.assertEquals(result, Long.MAX_VALUE);
    }

    @Test
    public void estimateSizeNoneRemaining() throws Exception {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                .setBody("[]");

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String authorizationHeader = "header";
            String userAgent = "userAgent";
            String path = "/api/endpoint";

            String url = server.url(path).toString();

            GitHubPageIterator<?> iterator = GitHubPageIterator.gson(url, () -> authorizationHeader, userAgent);

            // First should pass with original response
            try {
                iterator.next();

                long result = iterator.estimateSize();

                Assert.assertEquals(result, 0L);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request.getPath(), path);
            }
        }
    }

    @Test
    public void estimateSize() throws Exception {
        String path = "/api/endpoint";

        try (MockWebServer server = new MockWebServer()) {
            server.start();

            Collection<String> firstPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 1, 2, 2);
            Collection<String> secondPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 2, 2, 2);

            MockResponse response1 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("1", "2"));

            firstPageLinks.forEach(link -> response1.addHeader("Link", link));

            MockResponse response2 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("3", "4"));

            secondPageLinks.forEach(link -> response2.addHeader("Link", link));

            server.enqueue(response1);
            server.enqueue(response2);

            String authorizationHeader = "header";
            String userAgent = "userAgent";

            String url = server.url(path).toString();

            GitHubPageIterator<String> iterator = GitHubPageIterator.gson(url, () -> authorizationHeader, userAgent)
                    .map(JsonElement::getAsString);

            // First should pass with original response
            try {
                iterator.next();

                long result = iterator.estimateSize();

                Assert.assertEquals(result, 2L);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request1.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request1.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request1.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request1.getPath(), path);
            }
        }
    }

    @Test
    public void estimateSizeEstimateOnPage() throws Exception {
        String path = "/api/endpoint";

        try (MockWebServer server = new MockWebServer()) {
            server.start();

            Collection<String> firstPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 1, 2, 2);
            Collection<String> secondPageLinks = LinkHeaderTestSupport.getLinkHeaders(server, path, 2, 2, 2);

            MockResponse response1 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("1", "2"));

            firstPageLinks.forEach(link -> response1.addHeader("Link", link));

            MockResponse response2 = new MockResponse()
                    .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                    .setBody(getResponseContent("3"));

            secondPageLinks.forEach(link -> response2.addHeader("Link", link));

            server.enqueue(response1);
            server.enqueue(response2);

            String authorizationHeader = "header";
            String userAgent = "userAgent";

            String url = server.url(path).toString();

            GitHubPageIterator<String> iterator = GitHubPageIterator.gson(url, () -> authorizationHeader, userAgent)
                    .map(JsonElement::getAsString);

            // First should pass with original response
            try {
                iterator.next();

                long result = iterator.estimateSize();

                Assert.assertEquals(result, 2L);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request1 = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request1.getHeader("User-Agent"), userAgent);
                Assert.assertEquals(request1.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request1.getHeader("Authorization"), authorizationHeader);
                Assert.assertEquals(request1.getPath(), path);
            }
        }
    }

    private String getResponseContent(String... expected) {
        return "[" + Stream.of(expected).map(v -> "\"" + v + "\"").collect(Collectors.joining(", ")) + "]";
    }

}
