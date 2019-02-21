/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.paging;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.starchartlabs.alloy.core.collections.MoreSpliterators;
import org.starchartlabs.alloy.core.collections.PageIterator;
import org.starchartlabs.calamari.core.MediaTypes;
import org.starchartlabs.calamari.core.ResponseConditions;
import org.starchartlabs.calamari.core.exception.GitHubResponseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * An implementation of {@link PageIterator} for traversing paged data read from GitHub APIs
 *
 * <p>
 * Allows mapping of read elements at the Iterator level to prevent overhead in later steps
 *
 * <p>
 * Uses paging links to estimate remaining size
 *
 * <p>
 * See {@link MoreSpliterators#ofPaged(PageIterator)} for a path to consuming paged data as a Java Stream via
 * spliterator
 *
 * @author romeara
 *
 * @param <T>
 *            Type representing an individual paged element
 * @since 0.3.0
 * @see MoreSpliterators#ofPaged(PageIterator)
 */
public class GitHubPageIterator<T> implements PageIterator<T> {

    private final Supplier<String> authorizationHeader;

    private final String userAgent;

    private final JsonArrayConverter<?, T> itemMapper;

    private final OkHttpClient httpClient;

    private String url;

    private String mediaType;

    private Optional<Long> remainingEstimate;

    /**
     * Creates a new {@link GitHubPageIterator}
     *
     * @param url
     *            The initial URL to request paged data from
     * @param authorizationHeader
     *            Supplier which provides contents for the {@code Authorization} header when making requests
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param jsonDeserializer
     *            Function which transforms a raw JSON response representing a full page into individual data elements
     * @since 0.3.0
     */
    public GitHubPageIterator(String url, Supplier<String> authorizationHeader, String userAgent,
            Function<String, Collection<T>> jsonDeserializer) {
        this(url, authorizationHeader, userAgent, new JsonArrayConverter<>(jsonDeserializer, Function.identity()),
                MediaTypes.APP_PREVIEW);
    }

    /**
     * Creates a new {@link GitHubPageIterator}
     *
     * @param url
     *            The initial URL to request paged data from
     * @param authorizationHeader
     *            Supplier which provides contents for the {@code Authorization} header when making requests
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param jsonDeserializer
     *            Function which transforms a raw JSON response representing a full page into individual data elements
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     * @since 0.3.0
     */
    public GitHubPageIterator(String url, Supplier<String> authorizationHeader, String userAgent,
            Function<String, Collection<T>> jsonDeserializer, String mediaType) {
        this(url, authorizationHeader, userAgent, new JsonArrayConverter<>(jsonDeserializer, Function.identity()),
                mediaType);
    }

    /**
     * Creates a new {@link GitHubPageIterator}
     *
     * @param url
     *            The initial URL to request paged data from
     * @param authorizationHeader
     *            Supplier which provides contents for the {@code Authorization} header when making requests
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param itemMapper
     *            Function which transforms page elements to the representation desired by clients
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     */
    private GitHubPageIterator(String url, Supplier<String> authorizationHeader, String userAgent,
            JsonArrayConverter<?, T> itemMapper, String mediaType) {
        this.authorizationHeader = Objects.requireNonNull(authorizationHeader);
        this.userAgent = Objects.requireNonNull(userAgent);
        this.url = Objects.requireNonNull(url);
        this.itemMapper = Objects.requireNonNull(itemMapper);
        this.mediaType = Objects.requireNonNull(mediaType);

        httpClient = new OkHttpClient();
        remainingEstimate = Optional.empty();
    }

    /**
     * Transforms each element of paged data encountered via the provided function
     *
     * @param mapperPerElement
     *            Function to apply to each element representation, for transforming from the current element
     *            representation to a new one
     * @param <S>
     *            New type representing an individual paged element
     * @return A GitHubPageInterator which will provide elements as the desired representation
     * @since 0.3.0
     */
    public <S> GitHubPageIterator<S> map(Function<T, S> mapperPerElement) {
        Objects.requireNonNull(mapperPerElement);

        return new GitHubPageIterator<>(url, authorizationHeader, userAgent, itemMapper.andThenEach(mapperPerElement));
    }

    @Override
    public boolean hasNext() {
        return url != null;
    }

    @Override
    public Collection<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more pages may be read from the provided GitHub endpoint");
        }

        try {
            // Populate next set of elements, if possible
            Response response = getResponse(url);

            if (!response.isSuccessful()) {
                ResponseConditions.checkRateLimit(response);

                throw new GitHubResponseException("Response returned unsuccessfully (" + response.code() + ")");
            }

            // Update tracking of paging position (URL and paging links)
            PagingLinks pagingLinks = new PagingLinks(response.headers("Link"));
            url = pagingLinks.getNextPageUrl().orElse(null);

            remainingEstimate = estimateRemaining(pagingLinks);

            // Update cache of previously read elements, which will be read from until the next page is needed
            try (ResponseBody responseBody = response.body()) {
                return itemMapper.apply(responseBody.string());
            }
        } catch (IOException e) {
            throw new GitHubResponseException("Error reading response from GitHub", e);
        }
    }

    @Override
    public PageIterator<T> trySplit() {
        // TODO romeara Implementation of this will require further investigation
        // At minimum, logic to allow ending the iteration before a "next" link is absent would be needed in order to
        // sub-divide pages. For a first implementation iteration, this will be kept simpler and less bug-prone by not
        // allowing splitting
        return null;
    }

    @Override
    public long estimateSize() {
        return remainingEstimate
                .orElse(Long.MAX_VALUE);
    }

    /**
     * Creates a new {@link GitHubPageIterator} configured to extract paged elements into Gson {@link JsonElement}
     * instances
     *
     * @param url
     *            The initial URL to request paged data from
     * @param authorizationHeader
     *            Supplier which provides contents for the {@code Authorization} header when making requests
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @return A GitHubPageIterator which allows iteration over elements as JsonElement instances
     * @since 0.3.0
     */
    public static GitHubPageIterator<JsonElement> gson(String url, Supplier<String> authorizationHeader,
            String userAgent) {
        return new GitHubPageIterator<>(url, authorizationHeader, userAgent, new JsonElementConverter());
    }

    /**
     * Creates a new {@link GitHubPageIterator} configured to extract paged elements into Gson {@link JsonElement}
     * instances
     *
     * @param url
     *            The initial URL to request paged data from
     * @param authorizationHeader
     *            Supplier which provides contents for the {@code Authorization} header when making requests
     * @param userAgent
     *            The user agent to make web requests as, as
     *            <a href="https://developer.github.com/v3/#user-agent-required">required by GitHub</a>
     * @param mediaType
     *            The media type to request from the server via {@code Accept} header
     * @return A GitHubPageIterator which allows iteration over elements as JsonElement instances
     * @since 0.3.0
     */
    public static GitHubPageIterator<JsonElement> gson(String url, Supplier<String> authorizationHeader,
            String userAgent, String mediaType) {
        return new GitHubPageIterator<>(url, authorizationHeader, userAgent, new JsonElementConverter(), mediaType);
    }

    /**
     * Generates and executes a request to the provided URL with the configured user agent, media type, and
     * authorization header
     *
     * @param url
     *            The URL to make a request to
     * @return Representation of the response provided from the server
     * @throws IOException
     *             If there is an issue making the request to the server (connection issue, timeout, etc)
     */
    private Response getResponse(String url) throws IOException {
        Objects.requireNonNull(url);

        Request request = new Request.Builder()
                .get()
                .header("User-Agent", userAgent)
                .header("Accept", mediaType)
                .header("Authorization", authorizationHeader.get())
                .url(url)
                .build();

        return httpClient.newCall(request).execute();
    }

    /**
     * Estimates the maximum number of elements which may be remaining based on the page read and the next page to read
     *
     * <p>
     * This function notable over-estimates - it provides the maximum possible number of elements, assuming that even
     * the last page will contain exactly as many elements as were request per-page
     *
     * @param pagingLinks
     *            Representation of the GitHub links used to traverse paged responses
     * @return A high-ball estimate of the remaining elements, or empty if not enough information is available to make a
     *         reasonable estimate
     */
    private Optional<Long> estimateRemaining(PagingLinks pagingLinks) {
        Objects.requireNonNull(pagingLinks);

        Integer result = null;

        if (hasNext()) {
            Optional<Integer> perPage = Optional.ofNullable(pagingLinks)
                    .flatMap(PagingLinks::getNextPageUrl)
                    .flatMap(PagingLinks::getPerPage);

            Optional<Integer> nextPageNumber = Optional.ofNullable(pagingLinks)
                    .flatMap(PagingLinks::getNextPageUrl)
                    .flatMap(PagingLinks::getPage);

            Optional<Integer> lastPageNumber = Optional.ofNullable(pagingLinks)
                    .flatMap(PagingLinks::getLastPageUrl)
                    .flatMap(PagingLinks::getPage);

            if (perPage.isPresent() && nextPageNumber.isPresent() && lastPageNumber.isPresent()) {
                // The plus one accounts for having not read the next page
                int pagesRemaining = lastPageNumber.get() - nextPageNumber.get() + 1;

                result = pagesRemaining * perPage.get();
            }
        } else {
            result = 0;
        }

        return Optional.ofNullable(result).map(Integer::longValue);
    }

    /**
     * Function implementation which allows chaining of additional functions to transform the individual elements of a
     * paged response
     *
     * @author romeara
     *
     * @param <S>
     *            The starting representation of a given element
     * @param <T>
     *            The current provided implementation of the given element
     */
    private static final class JsonArrayConverter<S, T> implements Function<String, Collection<T>> {

        private final Function<String, Collection<S>> jsonDeserializer;

        private final Function<S, T> mapperPerElement;

        public JsonArrayConverter(Function<String, Collection<S>> jsonDeserializer, Function<S, T> mapperPerElement) {
            this.jsonDeserializer = Objects.requireNonNull(jsonDeserializer);
            this.mapperPerElement = Objects.requireNonNull(mapperPerElement);
        }

        @Override
        public Collection<T> apply(String json) {
            return jsonDeserializer.apply(json).stream()
                    .map(mapperPerElement)
                    .collect(Collectors.toList());
        }

        public <U> JsonArrayConverter<S, U> andThenEach(Function<T, U> mapperPerElement) {
            return new JsonArrayConverter<>(jsonDeserializer, this.mapperPerElement.andThen(mapperPerElement));
        }

    }

    /**
     * Converter used in {@link GitHubPageIterator#gson(String, Supplier, String)} call to support basic conversion from
     * JSON string to JSON elements from the array
     *
     * <p>
     * A full implementation is provided to avoid re-instantiating an instance of {@link Gson} every invocation
     *
     * @author romeara
     */
    private static final class JsonElementConverter implements Function<String, Collection<JsonElement>> {

        private Gson gson;

        public JsonElementConverter() {
            gson = new GsonBuilder().create();
        }

        @Override
        public Collection<JsonElement> apply(String json) {
            JsonElement element = gson.fromJson(json, JsonElement.class);

            return StreamSupport.stream(element.getAsJsonArray().spliterator(), false)
                    .collect(Collectors.toList());
        }

    }

}