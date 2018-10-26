/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.exception;

/**
 * Represents an error during the process of generating keys for authentication in GitHub web requests
 *
 * @author romeara
 * @since 0.1.0
 */
public class KeyLoadingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     *            Description of the exceptional condition which could not be recovered from
     * @since 0.1.0
     */
    public KeyLoadingException(String message) {
        super(message);
    }

    /**
     * @param message
     *            Description of the exceptional condition which could not be recovered from
     * @param cause
     *            The root cause of the error
     * @since 0.1.0
     */
    public KeyLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

}
