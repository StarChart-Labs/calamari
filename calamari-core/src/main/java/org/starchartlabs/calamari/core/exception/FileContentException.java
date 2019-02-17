/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.exception;

/**
 * Represents an error during the process of reading/deserializing file content stored on GitHub
 *
 * @author romeara
 * @since 0.3.0
 */
public class FileContentException extends RuntimeException {

    private static final long serialVersionUID = 1999424194651574510L;

    /**
     * @param message
     *            Description of the exceptional condition which could not be recovered from
     * @since 0.3.0
     */
    public FileContentException(String message) {
        super(message);
    }

    /**
     * @param message
     *            Description of the exceptional condition which could not be recovered from
     * @param cause
     *            The root cause of the error
     * @since 0.3.0
     */
    public FileContentException(String message, Throwable cause) {
        super(message, cause);
    }

}
