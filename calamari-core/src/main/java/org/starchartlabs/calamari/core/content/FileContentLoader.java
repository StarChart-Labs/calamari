/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starchartlabs.alloy.core.Preconditions;
import org.starchartlabs.alloy.core.Strings;
import org.starchartlabs.calamari.core.MediaTypes;
import org.starchartlabs.calamari.core.ResponseConditions;
import org.starchartlabs.calamari.core.auth.InstallationAccessToken;
import org.starchartlabs.calamari.core.exception.FileContentException;
import org.starchartlabs.calamari.core.exception.GitHubResponseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Represents handling for reading the contents of a configuration file from a GitHub repository
 *
 * <p>
 * It is intended for an application to create a single loader instance for a configuration file, and utilize
 * {@link #loadContents(InstallationAccessToken, String, String, String)} for each individual repository lookup desired
 *
 * <p>
 * If used by a GitHub App, access to the GitHub APIs used requires "contents:read" or "single file:read" permission(s)
 *
 * @author romeara
 * @since 0.3.0
 */
public class FileContentLoader {

    /** Logger reference to output information to the application log files */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OkHttpClient httpClient;

    private final String userAgent;

    private final String mediaType;

    /**
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @since 0.3.0
     */
    public FileContentLoader(String userAgent) {
        this(userAgent, MediaTypes.APP_PREVIEW);
    }

    /**
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     * @since 0.3.0
     */
    public FileContentLoader(String userAgent, String mediaType) {
        this.userAgent = Objects.requireNonNull(userAgent);
        this.mediaType = Objects.requireNonNull(mediaType);

        httpClient = new OkHttpClient();
    }

    /**
     * Reads contents of the configuration file specified at construction as per the
     * <a href="https://developer.github.com/v3/repos/contents/">GitHub file content API specification</a>
     *
     * @param installationToken
     *            Token specific to an application/repository authorizing a GitHub App to take actions on GitHub
     * @param repositoryUrl
     *            The URL of the repository to read configuration file contents from
     * @param ref
     *            The branch/tag to read contents from
     * @param path
     *            The repository-root relative path to the configuration file to read when loading contents
     * @return Plain-text file contents, if the file existed in the repository on the given branch/tag
     * @since 0.3.0
     */
    public Optional<String> loadContents(InstallationAccessToken installationToken, String repositoryUrl, String ref,
            String path) {
        Objects.requireNonNull(installationToken);
        Objects.requireNonNull(repositoryUrl);
        Objects.requireNonNull(ref);
        Objects.requireNonNull(path);

        String result = null;
        String responseBody = null;
        Request request = createRequest(installationToken, repositoryUrl, ref, path);

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try (ResponseBody body = response.body()) {
                    responseBody = body.string();
                    result = deserializeResponse(responseBody);
                }
            } else if (response.code() != 404) {
                ResponseConditions.checkRateLimit(response);

                throw new GitHubResponseException(
                        "Request unsuccessful (" + response.code() + " - " + response.message() + ")");
            }

            return Optional.ofNullable(result);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Error reading contents: {}", responseBody);

            throw new FileContentException(
                    "Error requesting or deserializing GitHub file content response.", e);
        }
    }

    /**
     * Generates an HTTP request representation for the given repository
     *
     * @param installationToken
     *            Token specific to an application/repository authorizing a GitHub App to take actions on GitHub
     * @param repositoryUrl
     *            The URL of the repository to read configuration file contents from
     * @param ref
     *            The branch/tag to read contents from
     * @param path
     *            The repository-root relative path to the configuration file to read when loading contents
     * @return HTTP request for the repository, including authorization headers
     */
    private Request createRequest(InstallationAccessToken installationToken, String repositoryUrl, String ref,
            String path) {
        HttpUrl url = HttpUrl.parse(repositoryUrl).newBuilder()
                .addEncodedPathSegment("contents")
                .addPathSegments(path)
                .addQueryParameter("ref", ref)
                .build();

        return new Request.Builder()
                .get()
                .header("Authorization", installationToken.get())
                .header("Accept", mediaType)
                .header("User-Agent", userAgent)
                .url(url)
                .build();
    }

    /**
     * Takes a raw JSON response body from GitHub and deserializes it into plain text
     *
     * @param responseBody
     *            JSON-encoded response
     * @return Plain text file contents
     */
    private String deserializeResponse(String responseBody) {
        Objects.requireNonNull(responseBody);

        String result = null;
        ContentResponse content = ContentResponse.fromJson(responseBody);

        Preconditions.checkArgument(Objects.equals(content.getEncoding(), "base64"),
                Strings.format(
                        "GitHub content responses are expected to be of base64 encoding, got %s - check that requested path is a file",
                        content.getEncoding()));

        try {
            // Note: Both UTF-8 and the mime-decoder were determined experimentally
            // For discussion and write-up, see:
            // https://stackoverflow.com/questions/40768678/decoding-base64-while-using-github-api-to-download-a-file/54302528#54302528
            result = new String(
                    Base64.getMimeDecoder().decode(content.getContent().getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            logger.error("Error deserializing base64 from response: {}", responseBody, e);

            throw new FileContentException("Error deserializing base64 from GitHub response", e);
        }

        return result;
    }

    /**
     * Represents JSON structure provided by GitHub for file contents
     *
     * @author romeara
     */
    private static final class ContentResponse {

        private static final Gson GSON = new GsonBuilder().create();

        @SerializedName("encoding")
        private final String encoding;

        @SerializedName("content")
        private final String content;

        @SuppressWarnings("unused")
        public ContentResponse(String encoding, String content) {
            this.encoding = Objects.requireNonNull(encoding);
            this.content = Objects.requireNonNull(content);
        }

        public String getEncoding() {
            return encoding;
        }

        public String getContent() {
            return content;
        }

        public static ContentResponse fromJson(String json) {
            return GSON.fromJson(json, ContentResponse.class);
        }

    }

}
