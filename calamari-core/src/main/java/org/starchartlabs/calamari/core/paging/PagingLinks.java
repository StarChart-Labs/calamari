/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.paging;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starchartlabs.alloy.core.MoreObjects;

import okhttp3.HttpUrl;

/**
 * Handles parsing of GitHub paging headers into distinct links for traversal of paged data
 *
 * <p>
 * GitHub does not provide all headers on all returns. The first page only contains the "next" and "last" links. The
 * last page only contains the "first" and "prev" links. All other pages contain all four headers. If there is only one
 * page of data, none of the links are returned
 *
 * <p>
 * Based on <a href="https://developer.github.com/v3/#pagination">GitHub pagination documentation</a>
 *
 * @author romeara
 * @since 0.3.0
 */
public class PagingLinks {

    // Pattern which matches link header format returned by GitHub and allows extraction of the URL and rel key
    // Example value: <https://api.github.com/user/repos?page=1&per_page=100>; rel="first"
    // Example extraction: https://api.github.com/user/repos?page=1&per_page=100, first
    private static final Pattern LINK_PATTERN = Pattern
            .compile("\\A.*<([A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)>; rel=\"([A-Za-z0-9]*)\"");

    private static final String FIRST_PAGE_REL = "first";

    private static final String PREVIOUS_PAGE_REL = "prev";

    private static final String NEXT_PAGE_REL = "next";

    private static final String LAST_PAGE_REL = "last";

    private static final String PAGE_PARAMETER = "page";

    private static final String PER_PAGE_PARAMETER = "per_page";

    /** Logger reference to output information to the application log files */
    private static final Logger logger = LoggerFactory.getLogger(PagingLinks.class);

    private final Optional<String> firstPageUrl;

    private final Optional<String> previousPageUrl;

    private final Optional<String> nextPageUrl;

    private final Optional<String> lastPageUrl;

    /**
     * Parses one or more paging links from GitHub response headers
     *
     * @param links
     *            One or more "Link" header values. Supports separated headers and headers contains a CSV of multiple
     *            link entries
     * @since 0.3.0
     */
    public PagingLinks(Collection<String> links) {
        Objects.requireNonNull(links);

        logger.debug("Link Headers: {}", links);

        String firstUrl = null;
        String prevUrl = null;
        String nextUrl = null;
        String lastUrl = null;

        Collection<String> allLinks = links.stream()
                .flatMap(s -> Arrays.asList(s.split(",")).stream())
                .map(String::trim)
                .collect(Collectors.toSet());

        for (String link : allLinks) {
            Matcher matcher = LINK_PATTERN.matcher(link);

            if (matcher.matches()) {
                String href = matcher.group(1);
                String rel = matcher.group(2);

                if (Objects.equals(rel, FIRST_PAGE_REL)) {
                    firstUrl = href;
                } else if (Objects.equals(rel, PREVIOUS_PAGE_REL)) {
                    prevUrl = href;
                } else if (Objects.equals(rel, NEXT_PAGE_REL)) {
                    nextUrl = href;
                } else if (Objects.equals(rel, LAST_PAGE_REL)) {
                    lastUrl = href;
                }
            }
        }

        firstPageUrl = Optional.ofNullable(firstUrl);
        previousPageUrl = Optional.ofNullable(prevUrl);
        nextPageUrl = Optional.ofNullable(nextUrl);
        lastPageUrl = Optional.ofNullable(lastUrl);
    }

    /**
     * @return URL to the first set of data in a paged sequence. Only present if the response providing the links was
     *         not the first page
     * @since 0.3.0
     */
    public Optional<String> getFirstPageUrl() {
        return firstPageUrl;
    }

    /**
     * @return URL to the previous set of data in a paged sequence. Only present if the response providing the links was
     *         not the first page
     * @since 0.3.0
     */
    public Optional<String> getPreviousPageUrl() {
        return previousPageUrl;
    }

    /**
     * @return URL to the next set of data in a paged sequence. Only present if the response providing the links was not
     *         the last page
     * @since 0.3.0
     */
    public Optional<String> getNextPageUrl() {
        return nextPageUrl;
    }

    /**
     * @return URL to the last set of data in a paged sequence. Only present if the response providing the links was not
     *         the last page
     * @since 0.3.0
     */
    public Optional<String> getLastPageUrl() {
        return lastPageUrl;
    }

    /**
     * Reads the page index from a link. Page's are 1-indexed
     *
     * @param url
     *            The link URL to read page information from
     * @return The page index, or empty if no valid per-page specification was found
     * @since 0.3.0
     */
    public static Optional<Integer> getPage(String url) {
        Objects.requireNonNull(url);

        return Optional.ofNullable(HttpUrl.get(url).queryParameter(PAGE_PARAMETER))
                .flatMap(PagingLinks::getInteger);
    }

    /**
     * Reads the number of elements request per-page from a link
     *
     * @param url
     *            The link URL to read per-page information from
     * @return The number of elements per page, or empty if no valid per-page specification was found
     * @since 0.3.0
     */
    public static Optional<Integer> getPerPage(String url) {
        Objects.requireNonNull(url);

        return Optional.ofNullable(HttpUrl.get(url).queryParameter(PER_PAGE_PARAMETER))
                .flatMap(PagingLinks::getInteger);
    }

    /**
     * Converts a URL parameter expected to be an integer, if possible
     *
     * @param candidate
     *            String to convert
     * @return The integer value, or empty if the provided string was non-numeric
     */
    private static Optional<Integer> getInteger(String candidate) {
        Objects.requireNonNull(candidate);

        Integer result = null;

        try {
            result = Integer.valueOf(candidate);
        } catch (NumberFormatException e) {
            // The way this function is used, this should not occur
            logger.warn("Non-numeric parameter value encountered: {}", candidate);
        }

        return Optional.ofNullable(result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirstPageUrl(),
                getPreviousPageUrl(),
                getNextPageUrl(),
                getLastPageUrl());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean result = false;

        if (obj instanceof PagingLinks) {
            PagingLinks compare = (PagingLinks) obj;

            result = Objects.equals(compare.getFirstPageUrl(), getFirstPageUrl())
                    && Objects.equals(compare.getPreviousPageUrl(), getPreviousPageUrl())
                    && Objects.equals(compare.getNextPageUrl(), getNextPageUrl())
                    && Objects.equals(compare.getLastPageUrl(), getLastPageUrl());
        }

        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("firstPageUrl", getFirstPageUrl())
                .add("previousPageUrl", getPreviousPageUrl())
                .add("nextPageUrl", getNextPageUrl())
                .add("lastPageUrl", getLastPageUrl())
                .toString();
    }

}
