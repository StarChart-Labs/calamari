/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;

import okhttp3.Response;

/**
 * Provides streamlined checking for common responses from GitHub
 *
 * @author romeara
 * @since 0.1.0
 */
public class ResponseConditions {

    private static final String RATE_LIMIT_MAXIMUM_HEADER = "X-RateLimit-Limit";

    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    /**
     * Analyzes a web response to determine if it represents exceeding GitHub's rate limits
     *
     * <p>
     * If the response does represent exceeded rate limiting, throws a {@link RequestLimitExceededException}
     *
     * <p>
     * To use this check with other web libraries, see {@link #checkRateLimit(Object, Function, BiFunction)}
     *
     * @param response
     *            OkHttp3 representation of a web response
     * @since 0.1.0
     */
    public static void checkRateLimit(Response response) throws RequestLimitExceededException {
        Objects.requireNonNull(response);

        checkRateLimit(response, Response::code, (res, header) -> res.headers().values(header));
    }

    /**
     * Analyzes a web response to determine if it represents exceeding GitHub's rate limits
     *
     * <p>
     * If the response does represent exceeded rate limiting, returns a {@link RequestLimitExceededException}
     *
     * <p>
     * Intended for use in streamed responses, such as a Mono. If the exception should be thrown immediately, use
     * {@link #checkRateLimit(Response)}
     *
     * <p>
     * To use this check with other web libraries, see {@link #validateRateLimit(Object, Function, BiFunction)}
     *
     * @param response
     *            OkHttp3 representation of a web response
     * @return A {@link RequestLimitExceededException} If the provided response represents exceeding GitHub's rate
     *         limiting, empty optional otherwise
     * @since 1.0.3
     */
    public static Optional<RequestLimitExceededException> validateRateLimit(Response response) {
        Objects.requireNonNull(response);

        return validateRateLimit(response, Response::code, (res, header) -> res.headers().values(header));
    }

    /**
     * Analyzes a web response to determine if it represents exceeding GitHub's rate limits
     *
     * <p>
     * If the response does represent exceeded rate limiting, throws a {@link RequestLimitExceededException}
     *
     * @param response
     *            Representation of a web response
     * @param codeLookup
     *            Function which reads the HTTP status code from the provided response
     * @param headerLookup
     *            Function which takes a response and header value as input, and produces all instances of that header
     *            from the response
     * @param <T>
     *            Java type representing a web response
     * @throws RequestLimitExceededException
     *             If the provided response represents exceeding GitHub's rate limiting
     * @since 0.1.0
     */
    public static <T> void checkRateLimit(T response, Function<T, Integer> codeLookup,
            BiFunction<T, String, Collection<String>> headerLookup) throws RequestLimitExceededException {
        Objects.requireNonNull(response);
        Objects.requireNonNull(codeLookup);
        Objects.requireNonNull(headerLookup);

        Optional<RequestLimitExceededException> rateLimitViolation = validateRateLimit(response, codeLookup,
                headerLookup);

        if (rateLimitViolation.isPresent()) {
            throw rateLimitViolation.get();
        }
    }

    /**
     * Analyzes a web response to determine if it represents exceeding GitHub's rate limits
     *
     * <p>
     * If the response does represent exceeded rate limiting, returns a {@link RequestLimitExceededException}
     * 
     * <p>
     * Intended for use in streamed responses, such as a Mono. If the exception should be thrown immediately, use
     * {@link #checkRateLimit(Object, Function, BiFunction)}
     *
     * @param response
     *            Representation of a web response
     * @param codeLookup
     *            Function which reads the HTTP status code from the provided response
     * @param headerLookup
     *            Function which takes a response and header value as input, and produces all instances of that header
     *            from the response
     * @param <T>
     *            Java type representing a web response
     * @return A {@link RequestLimitExceededException} If the provided response represents exceeding GitHub's rate
     *         limiting, empty optional otherwise
     * @since 1.0.3
     */
    public static <T> Optional<RequestLimitExceededException> validateRateLimit(T response,
            Function<T, Integer> codeLookup, BiFunction<T, String, Collection<String>> headerLookup) {
        Objects.requireNonNull(response);
        Objects.requireNonNull(codeLookup);
        Objects.requireNonNull(headerLookup);

        String limit = Optional.ofNullable(headerLookup.apply(response, RATE_LIMIT_MAXIMUM_HEADER))
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .orElse("(unknown)");

        String reset = Optional.ofNullable(headerLookup.apply(response, RATE_LIMIT_RESET_HEADER))
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .orElse("(unknown)");

        Collection<String> remaining = Optional.ofNullable(headerLookup.apply(response, RATE_LIMIT_REMAINING_HEADER))
                .orElse(Collections.emptyList());

        boolean rateLimitExceeded = Objects.equals(codeLookup.apply(response), 403) && remaining.contains("0");

        return rateLimitExceeded ? Optional.of(new RequestLimitExceededException(limit, reset)) : Optional.empty();
    }

}
