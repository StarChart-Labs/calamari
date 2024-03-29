/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.content;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.starchartlabs.calamari.core.MediaTypes;
import org.starchartlabs.calamari.core.auth.InstallationAccessToken;
import org.starchartlabs.calamari.core.content.FileContentLoader;
import org.starchartlabs.calamari.core.exception.GitHubResponseException;
import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class FileContentLoaderTest {

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    private static final Path TEST_RESOURCE_FOLDER = Paths.get("org", "starchartlabs", "calamari", "test", "core",
            "content");

    @Mock
    private InstallationAccessToken accessToken;

    private FileContentLoader fileContentLoader;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);

        fileContentLoader = new FileContentLoader("userAgent");
    }

    @AfterMethod
    public void teardown() throws Exception {
        Mockito.verifyNoMoreInteractions(accessToken);

        mocks.close();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullUserAgent() throws Exception {
        new FileContentLoader(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void contructNullUserAgentWithMediaType() throws Exception {
        new FileContentLoader(null, "mediaType");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullMediaType() throws Exception {
        new FileContentLoader("userAgent", null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void loadContentsNullAccessToken() throws Exception {
        fileContentLoader.loadContents(null, "repositoryUrl", "ref", "path.json");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void loadContentsNullRepositoryUrl() throws Exception {
        fileContentLoader.loadContents(accessToken, null, "ref", "path.json");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void loadContentsNullRef() throws Exception {
        fileContentLoader.loadContents(accessToken, "repositoryUrl", null, "path.json");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void loadContentsNullPath() throws Exception {
        fileContentLoader.loadContents(accessToken, "repositoryUrl", "ref", null);
    }

    @Test(expectedExceptions = GitHubResponseException.class)
    public void loadContentsErrorResponse() throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(412);

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent");

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                contentLoader.loadContents(accessToken, repositoryUrl, ref, path);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test(expectedExceptions = RequestLimitExceededException.class)
    public void loadContentsRateLimitExceeded() throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(403)
                .addHeader(RATE_LIMIT_REMAINING_HEADER, "0");

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent");

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                contentLoader.loadContents(accessToken, repositoryUrl, ref, path);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test
    public void loadContentsNotFound() throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(404);

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent");

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                Optional<String> result = contentLoader.loadContents(accessToken, repositoryUrl, ref, path);

                Assert.assertNotNull(result);
                Assert.assertFalse(result.isPresent());
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void loadContentUnexpectedEncoding() throws Exception {
        String responseJson = null;

        try (BufferedReader reader = getClasspathReader(
                TEST_RESOURCE_FOLDER.resolve("fileContentUnsupportedEncodingResponse.json"))) {
            responseJson = reader.lines()
                    .collect(Collectors.joining("\n"));
        }

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                .setBody(responseJson);

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent");

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                contentLoader.loadContents(accessToken, repositoryUrl, ref, path);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test
    public void loadContents() throws Exception {
        String responseJson = null;

        try (BufferedReader reader = getClasspathReader(TEST_RESOURCE_FOLDER.resolve("fileContentResponse.json"))) {
            responseJson = reader.lines()
                    .collect(Collectors.joining("\n"));
        }

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                .setBody(responseJson);

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent");

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                Optional<String> result = contentLoader.loadContents(accessToken, repositoryUrl, ref, path);

                Assert.assertNotNull(result);
                Assert.assertTrue(result.isPresent());
                Assert.assertEquals(result.get(), "This is test text");
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test
    public void loadContentsMimeEncoding() throws Exception {
        String responseJson = null;
        String expectedContents = "productionFiles:\n" +
                "   include:\n" +
                "      - '**/README*'\n" +
                "releaseNoteFiles:\n" +
                "   include:\n" +
                "      - '**/CHANGE*LOG*'\n" +
                "      - '**/RELEASE*NOTES*'\n";

        try (BufferedReader reader = getClasspathReader(
                TEST_RESOURCE_FOLDER.resolve("fileContentResponseMimeEncoding.json"))) {
            responseJson = reader.lines()
                    .collect(Collectors.joining("\n"));
        }

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", MediaTypes.APP_PREVIEW)
                .setBody(responseJson);

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent");

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                Optional<String> result = contentLoader.loadContents(accessToken, repositoryUrl, ref, path);

                Assert.assertNotNull(result);
                Assert.assertTrue(result.isPresent());
                Assert.assertEquals(result.get(), expectedContents);
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), MediaTypes.APP_PREVIEW);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test
    public void loadContentsCustomMediaType() throws Exception {
        String responseJson = null;

        try (BufferedReader reader = getClasspathReader(TEST_RESOURCE_FOLDER.resolve("fileContentResponse.json"))) {
            responseJson = reader.lines()
                    .collect(Collectors.joining("\n"));
        }

        String mediaType = "customMediaType";

        MockResponse response = new MockResponse()
                .addHeader("Content-Type", mediaType)
                .setBody(responseJson);

        String owner = "owner";
        String repository = "repository";
        String ref = "ref";
        String path = "path.json";

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(response);
            server.start();

            String repositoryUrl = server.url("/api/repos/" + owner + "/" + repository).toString();

            FileContentLoader contentLoader = new FileContentLoader("userAgent", mediaType);

            Mockito.when(accessToken.get()).thenReturn("token authToken12345");

            try {
                Optional<String> result = contentLoader.loadContents(accessToken, repositoryUrl, ref, path);

                Assert.assertNotNull(result);
                Assert.assertTrue(result.isPresent());
                Assert.assertEquals(result.get(), "This is test text");
            } finally {
                Assert.assertEquals(server.getRequestCount(), 1);
                RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

                Assert.assertEquals(request.getHeader("User-Agent"), "userAgent");
                Assert.assertEquals(request.getHeader("Accept"), mediaType);
                Assert.assertEquals(request.getHeader("Authorization"), "token authToken12345");
                Assert.assertEquals(request.getPath(),
                        "/api/repos/" + owner + "/" + repository + "/contents/" + path + "?ref=" + ref);

                Mockito.verify(accessToken).get();
            }
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void decodeFileContentNullEncoding() throws Exception {
        String encodedContent = "VGhpcyBpcyB0ZXN0IHRleHQ=";

        FileContentLoader.decodeFileContent(null, encodedContent);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void decodeFileContentNullEncodedContents() throws Exception {
        String encoding = "base64";

        FileContentLoader.decodeFileContent(encoding, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void decodeFileContentUnsupportedEncoding() throws Exception {
        String encoding = "base6";
        String encodedContent = "VGhpcyBpcyB0ZXN0IHRleHQ=";

        FileContentLoader.decodeFileContent(encoding, encodedContent);
    }

    @Test
    public void decodeFileContent() throws Exception {
        String encoding = "base64";
        String encodedContent = "VGhpcyBpcyB0ZXN0IHRleHQ=";
        String expectedContents = "This is test text";

        String result = FileContentLoader.decodeFileContent(encoding, encodedContent);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, expectedContents);
    }

    @Test
    public void decodeFileContentMimeEncoding() throws Exception {
        String encoding = "base64";
        String encodedContent = "cHJvZHVjdGlvbkZpbGVzOgogICBpbmNsdWRlOgogICAgICAtICcqKi9SRUFETUUqJwpyZWxlYXNlTm90ZUZpbGVzOgogICBpbmNsdWRlOgogICAgICAtICcqKi9DSEFOR0UqTE9HKicKICAgICAgLSAnKiovUkVMRUFTRSpOT1RFUyonCg==";
        String expectedContents = "productionFiles:\n" +
                "   include:\n" +
                "      - '**/README*'\n" +
                "releaseNoteFiles:\n" +
                "   include:\n" +
                "      - '**/CHANGE*LOG*'\n" +
                "      - '**/RELEASE*NOTES*'\n";

        String result = FileContentLoader.decodeFileContent(encoding, encodedContent);

        Assert.assertNotNull(result);
        Assert.assertEquals(result, expectedContents);
    }

    private BufferedReader getClasspathReader(Path filePath) {
        return new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath.toString()),
                        StandardCharsets.UTF_8));
    }

}
