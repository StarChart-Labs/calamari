/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.exception;

/**
 * Represents an error during the process of communicating with GitHub APIs
 *
 * @author romeara
 * @since 0.3.0
 */
public class GitHubResponseException extends RuntimeException {

    private static final long serialVersionUID = 1702104867544399789L;

    /**
     * @param message
     *            Description of the exceptional condition which could not be recovered from
     * @since 0.3.0
     */
    public GitHubResponseException(String message) {
        super(message);
    }

    /**
     * @param message
     *            Description of the exceptional condition which could not be recovered from
     * @param cause
     *            The root cause of the error
     * @since 0.3.0
     */
    public GitHubResponseException(String message, Throwable cause) {
        super(message, cause);
    }

}
