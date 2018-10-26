/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core;

/**
 * Represents various data format identifiers which may be sent or requested to/from GitHub via Accept/Content-Type
 * headers in web requests
 *
 * @author romeara
 * @since 0.1.0
 */
public final class MediaTypes {

    /**
     * Media type used by GitHub app's during the preview period
     *
     * @since 0.1.0
     */
    public static final String APP_PREVIEW = "application/vnd.github.machine-man-preview+json";

    /**
     * Prevent instantiation of utility class
     */
    private MediaTypes() throws InstantiationException {
        throw new InstantiationException("Cannot instantiate instance of utility class '" + getClass().getName() + "'");
    }

}
