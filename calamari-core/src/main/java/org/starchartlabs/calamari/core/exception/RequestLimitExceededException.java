/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.exception;

import org.starchartlabs.alloy.core.Strings;

/**
 * Represents a request to GitHub which failed due to the calling application exceeding allotted its rate limits
 *
 * <p>
 * See the <a href="https://developer.github.com/v3/#rate-limiting">GitHub API rate limit documentation</a>
 *
 * @author romeara
 * @since 0.1.0
 */
public class RequestLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 3619506905378504972L;

    /**
     * @param limit
     *            The number of requests allowed per time period
     * @param reset
     *            The amount of time before the number of allowed requests resets
     * @since 0.1.0
     */
    public RequestLimitExceededException(String limit, String reset) {
        super(Strings.format("Maximum requests to GitHub exceeded. Limit of %s, resets at %s", limit, reset));
    }

    /**
     * @param limit
     *            The number of requests allowed per time period
     * @param reset
     *            The amount of time before the number of allowed requests resets
     * @param cause
     *            The root cause that indicated an error occurred
     * @since 0.1.0
     */
    public RequestLimitExceededException(String limit, String reset, Throwable cause) {
        super(Strings.format("Maximum requests to GitHub exceeded. Limit of %s, resets at %s", limit, reset), cause);
    }

}
