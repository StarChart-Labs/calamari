/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.starchartlabs.calamari.core.MediaTypes;
import org.starchartlabs.calamari.core.auth.ApplicationKey;
import org.starchartlabs.calamari.core.auth.InstallationAccessToken;
import org.starchartlabs.calamari.core.exception.KeyLoadingException;
import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class InstallationAccessTokenTest {

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    private static final Path TEST_RESOURCE_FOLDER = Paths.get("org", "starchartlabs", "calamari", "test", "core",
            "auth");

    private static final Gson GSON = new GsonBuilder().create();

    private ApplicationKey applicationKey;

    private String accessToken;

    private String accessTokenResponse;

    @BeforeClass
    public void setup() {
        applicationKey = new ApplicationKey("gitHubAppId", this::readPrivateKey);
        accessToken = "authorizationToken";

        JsonObject json = new JsonObject();
        json.addProperty("token", accessToken);

        accessTokenResponse = GSON.toJson(json);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructDefaultMediaAndCacheNullInstallationAccessTokenUrl() throws Exception {
        new InstallationAccessToken(null, applicationKey, "userAgent");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructDefaultMediaAndCacheNullApplicationKey() throws Exception {
        new InstallationAccessToken("http://url", null, "userAgent");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructDefaultMediaAndCacheNullUserAgent() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructDefaultCacheNullInstallationAccessTokenUrl() throws Exception {
        new InstallationAccessToken(null, applicationKey, "userAgent", "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructDefaultCacheNullApplicationKey() throws Exception {
        new InstallationAccessToken("http://url", null, "userAgent", "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructDefaultCacheNullUserAgent() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, null, "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void contructDefaultCacheNullMediaType() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, "userAgent", null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullInstallationAccessTokenUrl() throws Exception {
        new InstallationAccessToken(null, applicationKey, "userAgent", "mediaType", 5);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullApplicationKey() throws Exception {
        new InstallationAccessToken("http://url", null, "userAgent", "mediaType", 5);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullUserAgent() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, null, "mediaType", 5);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void contructNullMediaType() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, "userAgent", null, 5);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void constructZeroCacheExpiration() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, "userAgent", "mediaType", 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void constructTooManyCacheExpiration() throws Exception {
        new InstallationAccessToken("http://url", applicationKey, "userAgent", "mediaType", 61);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void forRepositoryNullRepositoryUrl() throws Exception {
        InstallationAccessToken.forRepository(null, applicationKey, "userAgent");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void forRepositoryNullApplicationKey() throws Exception {
        InstallationAccessToken.forRepository("http://repo", null, "userAgent");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void forRepositoryNullUserAgent() throws Exception {
        InstallationAccessToken.forRepository("http://repo", applicationKey, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void forRepositoryNullMediaType() throws Exception {
        InstallationAccessToken.forRepository("http://repo", applicationKey, "userAgent", null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getInstallationUrlNullRepositoryUrl() throws Exception {
        InstallationAccessToken.getInstallationUrl(null, applicationKey, "userAgent");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getInstallationUrlNullApplicationKey() throws Exception {
        InstallationAccessToken.getInstallationUrl("http://repo", null, "userAgent");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getInstallationUrlNullUserAgent() throws Exception {
        InstallationAccessToken.getInstallationUrl("http://repo", applicationKey, null);
    }

    @Test(expectedExceptions = KeyLoadingException.class)
    public void getUnsuccessfulRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(404));
            server.start();

            String installationAccessTokenUrl = server.url("/install").toString();

            InstallationAccessToken token = new InstallationAccessToken(installationAccessTokenUrl, applicationKey,
                    "userAgent");

            try {
                token.get();
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getPath(), "/install");
            }
        }
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void getRequestLimitExceeded() throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(403)
                .addHeader(RATE_LIMIT_REMAINING_HEADER, "0");

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String installationAccessTokenUrl = server.url("/install").toString();

            InstallationAccessToken token = new InstallationAccessToken(installationAccessTokenUrl, applicationKey,
                    "userAgent");

            try {
                token.get();
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getPath(), "/install");
            }
        }
    }

    @Test
    public void get() throws Exception {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(accessTokenResponse);

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String installationAccessTokenUrl = server.url("/install").toString();

            InstallationAccessToken token = new InstallationAccessToken(installationAccessTokenUrl, applicationKey,
                    "userAgent");

            try {
                String result = token.get();

                Assert.assertNotNull(result);
                Assert.assertEquals(result, "token " + accessToken);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getPath(), "/install");
            }
        }
    }

    @Test
    public void getCached() throws Exception {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(accessTokenResponse);

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String installationAccessTokenUrl = server.url("/install").toString();

            InstallationAccessToken token = new InstallationAccessToken(installationAccessTokenUrl, applicationKey,
                    "userAgent");

            try {
                String result = token.get();

                Assert.assertNotNull(result);
                Assert.assertEquals(result, "token " + accessToken);

                // Default caching is 59-60 minutes - this should not result in any further web calls
                for (int i = 0; i < 10; i++) {
                    token.get();
                }
            } finally {
                // Caching results in a single web call
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getPath(), "/install");
            }
        }
    }

    @Test
    public void getCustomMediaType() throws Exception {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(accessTokenResponse);

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String installationAccessTokenUrl = server.url("/install").toString();

            InstallationAccessToken token = new InstallationAccessToken(installationAccessTokenUrl, applicationKey,
                    "userAgent", "mediaType");

            try {
                String result = token.get();

                Assert.assertNotNull(result);
                Assert.assertEquals(result, "token " + accessToken);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), "mediaType");
                Assert.assertEquals(request.getPath(), "/install");
            }
        }
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void forRepositoryRequestLimitExceeded() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String repoUrl = server.url("/repo").toString();

            MockResponse respositoryResponse = new MockResponse()
                    .setResponseCode(403)
                    .addHeader(RATE_LIMIT_REMAINING_HEADER, "0");

            server.enqueue(respositoryResponse);

            InstallationAccessToken.forRepository(repoUrl, applicationKey, "userAgent");
        }
    }

    @Test
    public void forRepository() throws Exception {
        MockResponse accessResponse = new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(accessTokenResponse);

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String repoUrl = server.url("/repo").toString();
            String installationAccessTokenUrl = server.url("/install").toString();

            MockResponse respositoryResponse = new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(getRepositoryResponse(installationAccessTokenUrl));

            server.enqueue(respositoryResponse);
            server.enqueue(accessResponse);


            InstallationAccessToken token = InstallationAccessToken.forRepository(repoUrl, applicationKey,
                    "userAgent");

            try {
                String result = token.get();

                Assert.assertNotNull(result);
                Assert.assertEquals(result, "token " + accessToken);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 2);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getPath(), "/repo/installation");

                request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getPath(), "/install");
            }
        }
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void getInstallationTokenUrlRequestLimitExceeded() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String repoUrl = server.url("/repo").toString();

            MockResponse respositoryResponse = new MockResponse()
                    .setResponseCode(403)
                    .addHeader(RATE_LIMIT_REMAINING_HEADER, "0");

            server.enqueue(respositoryResponse);

            InstallationAccessToken.getInstallationUrl(repoUrl, applicationKey, "userAgent");
        }
    }

    @Test
    public void getInstallationTokenUrl() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String repoUrl = server.url("/repo").toString();
            String installationAccessTokenUrl = server.url("/install").toString();

            MockResponse respositoryResponse = new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(getRepositoryResponse(installationAccessTokenUrl));

            server.enqueue(respositoryResponse);

            try {
                String result = InstallationAccessToken.getInstallationUrl(repoUrl, applicationKey, "userAgent");

                Assert.assertNotNull(result);
                Assert.assertEquals(result, installationAccessTokenUrl);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertNotNull(request.getHeader("Authorization"));
                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getPath(), "/repo/installation");
            }
        }
    }

    private String getRepositoryResponse(String installationUrl) {
        JsonObject json = new JsonObject();
        json.addProperty("access_tokens_url", installationUrl);

        return GSON.toJson(json);
    }

    private String readPrivateKey() {
        // Note: The test key was generated from a GitHub App, and immediately removed as a valid key, and so is not a
        // security issue
        try (BufferedReader reader = getClasspathReader(
                TEST_RESOURCE_FOLDER.resolve("orphaned-github-private-key.pem"))) {
            String key = reader.lines()
                    .collect(Collectors.joining("\n"));
            System.out.println(key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedReader getClasspathReader(Path filePath) {
        return new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath.toString()),
                        StandardCharsets.UTF_8));
    }

}
