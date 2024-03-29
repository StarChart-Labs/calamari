/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.auth;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.starchartlabs.alloy.core.Preconditions;
import org.starchartlabs.alloy.core.Strings;
import org.starchartlabs.alloy.core.Suppliers;
import org.starchartlabs.calamari.core.MediaTypes;
import org.starchartlabs.calamari.core.ResponseConditions;
import org.starchartlabs.calamari.core.exception.KeyLoadingException;
import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Represents an access token used to validate web requests to GitHub as a
 * <a href="https://developer.github.com/v3/apps/installations/">GitHub App installation</a>
 *
 * <p>
 * Handles logic for exchanging an application key for an installation-specific access token and caching them until
 * invalid, as described in
 * <a href= "https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/">authenticating as
 * a GitHub App</a>
 *
 * <p>
 * Uses Java {@link Supplier} pattern to allow re-generation of tokens as needed
 *
 * @author romeara
 * @since 0.1.0
 */
public class InstallationAccessToken implements Supplier<String> {

    // The maximum is 60, allow for some drift
    private static final int DEFAULT_EXPIRATION_MINUTES = 58;

    private static final Gson GSON = new GsonBuilder().create();

    private final ApplicationKey applicationKey;

    private final String installationAccessTokenUrl;

    private final String userAgent;

    private final String mediaType;

    private final OkHttpClient httpClient;

    private final Supplier<String> tokenSupplier;

    private final Supplier<String> headerSupplier;

    private final int cacheExpirationMinutes;

    /**
     * @param installationAccessTokenUrl
     *            URL which represents access token resources for a specific GitHub App installation
     * @param applicationKey
     *            Key used to access GitHub web resources as a GitHub App outside an installation context
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @since 0.1.0
     */
    public InstallationAccessToken(String installationAccessTokenUrl, ApplicationKey applicationKey, String userAgent) {
        this(installationAccessTokenUrl, applicationKey, userAgent, MediaTypes.APP_PREVIEW, DEFAULT_EXPIRATION_MINUTES);
    }

    /**
     * @param installationAccessTokenUrl
     *            URL which represents access token resources for a specific GitHub App installation
     * @param applicationKey
     *            Key used to access GitHub web resources as a GitHub App outside an installation context
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     * @since 0.2.0
     */
    public InstallationAccessToken(String installationAccessTokenUrl, ApplicationKey applicationKey,
            String userAgent, String mediaType) {
        this(installationAccessTokenUrl, applicationKey, userAgent, mediaType, DEFAULT_EXPIRATION_MINUTES);
    }

    /**
     * @param installationAccessTokenUrl
     *            URL which represents access token resources for a specific GitHub App installation
     * @param applicationKey
     *            Key used to access GitHub web resources as a GitHub App outside an installation context
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     * @param cacheExpirationMinutes
     *            Number of minutes to cache generated bearer tokens for authentication with GitHub, maximum 60
     * @since 0.4.0
     */
    public InstallationAccessToken(String installationAccessTokenUrl, ApplicationKey applicationKey,
            String userAgent, String mediaType, int cacheExpirationMinutes) {
        this.applicationKey = Objects.requireNonNull(applicationKey);
        this.installationAccessTokenUrl = Objects.requireNonNull(installationAccessTokenUrl);
        this.userAgent = Objects.requireNonNull(userAgent);
        this.mediaType = Objects.requireNonNull(mediaType);

        Preconditions.checkArgument(cacheExpirationMinutes > 0, "Must provide an expiration time greater than zero");
        Preconditions.checkArgument(cacheExpirationMinutes <= 60,
                "Must provide an expiration time less than or equal to 60");

        httpClient = new OkHttpClient();
        this.cacheExpirationMinutes = cacheExpirationMinutes;
        tokenSupplier = Suppliers.memoizeWithExpiration(this::generateNewToken, this.cacheExpirationMinutes,
                TimeUnit.MINUTES);
        headerSupplier = Suppliers.map(tokenSupplier, InstallationAccessToken::toAuthorizationHeader);
    }

    /**
     * @return Authorization header value to authenticate as a GitHub App installation
     * @throws KeyLoadingException
     *             If the is an error making the GitHub web request to obtain the access token
     * @since 0.1.0
     */
    @Override
    public String get() {
        return getHeader();
    }

    /**
     * 
     * @return Authorization header value to authenticate as a GitHub App installation
     * @throws KeyLoadingException
     *             If the is an error making the GitHub web request to obtain the access token
     * @since 1.2.2
     */
    public String getHeader() {
        return headerSupplier.get();
    }

    /**
     * @return Token used to authenticate as a GitHub App installation. It is recommended to use {@link #getHeader()}
     *         instead in most cases, which provides the fully formatted header values for API requests. The token
     *         itself should only be needed for operations such as native Git calls
     * @throws KeyLoadingException
     *             If the is an error making the GitHub web request to obtain the access token
     * @since 1.2.2
     */
    public String getToken() {
        return tokenSupplier.get();
    }

    /**
     * Generates a new access token from the application key reference and a known installation instance
     *
     * @return Generated access token valid for up to sixty minutes after this function is called
     * @throws RequestLimitExceededException
     *             If the request exceeded the maximum allowed requests to GitHub in a given time period
     * @throws KeyLoadingException
     *             If there is an error making the GitHub web request to obtain the access token
     */
    private String generateNewToken() {
        HttpUrl url = HttpUrl.parse(installationAccessTokenUrl);

        RequestBody requestBody = RequestBody.create(new byte[] {}, null);
        Request request = new Request.Builder()
                .post(requestBody)
                .header("Authorization", applicationKey.get())
                .header("Accept", mediaType)
                .header("User-Agent", userAgent)
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try (ResponseBody body = response.body()) {
                    return AccessTokenResponse.fromJson(body.string()).getToken();
                }
            } else {
                ResponseConditions.checkRateLimit(response);

                throw new KeyLoadingException(
                        Strings.format("Request exchanging application key for installation token failed (%s - %s)",
                                response.code(), response.message()));
            }
        } catch (IOException e) {
            throw new KeyLoadingException("Error requesting or deserializing GitHub installation token response", e);
        }
    }

    /**
     * Creates an installation access token for the installation on a given repository.
     *
     * <p>
     * Uses the provided {@code applicationKey} to read required installation details from GitHub specific to the
     * repository represented at the provided URL
     *
     * @param repositoryUrl
     *            The API URL which represents the target repository on GitHub
     * @param applicationKey
     *            Application key which allows authentication as a GitHub App in web requests
     * @param userAgent
     *            User agent to make repository requests with, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @return A reference to a renewable access token for authentication as a specific installation in web requests to
     *         GitHub
     * @throws RequestLimitExceededException
     *             If the request exceeded the maximum allowed requests to GitHub in a given time period
     * @since 0.1.0
     */
    public static InstallationAccessToken forRepository(String repositoryUrl, ApplicationKey applicationKey,
            String userAgent) {
        return forRepository(repositoryUrl, applicationKey, userAgent, MediaTypes.APP_PREVIEW);
    }

    /**
     * Creates an installation access token for the installation on a given repository.
     *
     * <p>
     * Uses the provided {@code applicationKey} to read required installation details from GitHub specific to the
     * repository represented at the provided URL
     *
     * @param repositoryUrl
     *            The API URL which represents the target repository on GitHub
     * @param applicationKey
     *            Application key which allows authentication as a GitHub App in web requests
     * @param userAgent
     *            User agent to make repository requests with, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     * @return A reference to a renewable access token for authentication as a specific installation in web requests to
     *         GitHub
     * @throws RequestLimitExceededException
     *             If the request exceeded the maximum allowed requests to GitHub in a given time period
     * @since 0.1.0
     */
    public static InstallationAccessToken forRepository(String repositoryUrl, ApplicationKey applicationKey,
            String userAgent, String mediaType) {
        Objects.requireNonNull(applicationKey);
        Objects.requireNonNull(repositoryUrl);
        Objects.requireNonNull(userAgent);
        Objects.requireNonNull(mediaType);

        String installationAccessTokenUrl = getInstallationUrl(repositoryUrl, applicationKey, userAgent);

        return new InstallationAccessToken(installationAccessTokenUrl, applicationKey, userAgent, mediaType);
    }

    /**
     * Looks up the location of the resource describing the installation for a given repository
     *
     * <p>
     * Uses the provided {@code applicationKey} to read required installation details from GitHub specific to the
     * repository represented at the provided URL. It is recommended that clients use
     * {@link #forRepository(String, ApplicationKey, String)} when possible - this call is primarily meant for cases
     * where the URL can be used with caching mechanisms to reduce the total number of calls to GitHub
     *
     * @param repositoryUrl
     *            The API URL which represents the target repository on GitHub
     * @param applicationKey
     *            Application key which allows authentication as a GitHub App in web requests
     * @param userAgent
     *            User agent to make repository requests with, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @return A reference to a resource describing the installation on the repository
     * @throws RequestLimitExceededException
     *             If the request exceeded the maximum allowed requests to GitHub in a given time period
     * @since 0.1.2
     */
    public static String getInstallationUrl(String repositoryUrl, ApplicationKey applicationKey, String userAgent) {
        Objects.requireNonNull(applicationKey);
        Objects.requireNonNull(repositoryUrl);
        Objects.requireNonNull(userAgent);

        OkHttpClient httpClient = new OkHttpClient();

        HttpUrl url = HttpUrl.parse(repositoryUrl).newBuilder()
                .addEncodedPathSegment("installation")
                .build();

        Request request = new Request.Builder()
                .get()
                .header("Authorization", applicationKey.get())
                .header("Accept", MediaTypes.APP_PREVIEW)
                .header("User-Agent", userAgent)
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try (ResponseBody body = response.body()) {
                    return InstallationResponse.fromJson(body.string()).getAccessTokensUrl();
                }
            } else {
                ResponseConditions.checkRateLimit(response);

                throw new RuntimeException(
                        "Request unsuccessful (" + response.code() + " - " + response.message() + ")");
            }
        } catch (IOException e) {
            throw new KeyLoadingException("Error requesting or deserializing GitHub installation response", e);
        }
    }

    /**
     * @param token
     *            Access token to use in requests to GitHub for authorization as a specific GitHub App installation
     * @return Value for the {@code Authorization} header of HTTP requests to authorize with the access token
     */
    private static String toAuthorizationHeader(String token) {
        Objects.requireNonNull(token);

        return Strings.format("token %s", token);
    }

    /**
     * Represents relevant parts of a JSON response from GitHub describing an App <a href=
     * "https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-an-installation">installation
     * access token</a>
     *
     * @author romeara
     */
    private static final class AccessTokenResponse {

        private final String token;

        @SuppressWarnings("unused")
        public AccessTokenResponse(String token) {
            this.token = Objects.requireNonNull(token);
        }

        public String getToken() {
            return token;
        }

        public static AccessTokenResponse fromJson(String json) {
            return GSON.fromJson(json, AccessTokenResponse.class);
        }

    }

    /**
     * Represents relevant parts of a JSON response from GitHub describing an App
     * <a href="https://developer.github.com/v3/apps/installations/">installation</a>
     *
     * @author romeara
     */
    private static final class InstallationResponse {

        @SerializedName("access_tokens_url")
        private final String accessTokensUrl;

        @SuppressWarnings("unused")
        public InstallationResponse(String accessTokensUrl) {
            this.accessTokensUrl = Objects.requireNonNull(accessTokensUrl);
        }

        public String getAccessTokensUrl() {
            return accessTokensUrl;
        }

        public static InstallationResponse fromJson(String json) {
            return GSON.fromJson(json, InstallationResponse.class);
        }

    }

}
