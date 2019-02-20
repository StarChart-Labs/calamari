/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test;

import java.util.ArrayList;
import java.util.Collection;

import okhttp3.mockwebserver.MockWebServer;

/**
 * Provides supporting operations for streamlining dynamic tests related to GitHub paging links
 *
 * @author romeara
 * @since 0.3.0
 */
public final class LinkHeaderTestSupport {

    private static final String FIRST_PAGE_REL = "first";

    private static final String PREV_PAGE_REL = "prev";

    private static final String NEXT_PAGE_REL = "next";

    private static final String LAST_PAGE_REL = "last";

    private static final String PAGE_PARAMETER = "page";

    private static final String PER_PAGE_PARAMETER = "per_page";

    /**
     * Prevent instantiation of utility class
     */
    private LinkHeaderTestSupport() throws InstantiationException {
        throw new InstantiationException("Cannot instantiate instance of utility class '" + getClass().getName() + "'");
    }

    public static Collection<String> getLinkHeaders(MockWebServer server, String path, int currentPage, int maxPage,
            int perPage) {
        Collection<String> result = new ArrayList<>();

        if (currentPage > 1) {
            int firstPage = 1;
            int previousPage = currentPage - 1;

            String firstPageLink = server.url(path).newBuilder()
                    .addQueryParameter(PAGE_PARAMETER, Integer.toString(firstPage))
                    .addQueryParameter(PER_PAGE_PARAMETER, Integer.toString(perPage))
                    .build()
                    .toString();

            String previousPageLink = server.url(path).newBuilder()
                    .addQueryParameter(PAGE_PARAMETER, Integer.toString(previousPage))
                    .addQueryParameter(PER_PAGE_PARAMETER, Integer.toString(perPage))
                    .build()
                    .toString();

            result.add(getLinkHeader(firstPageLink, FIRST_PAGE_REL));
            result.add(getLinkHeader(previousPageLink, PREV_PAGE_REL));
        }

        if (currentPage < maxPage) {
            int nextPage = currentPage + 1;
            int lastPage = maxPage;

            String nextPageLink = server.url(path).newBuilder()
                    .addQueryParameter(PAGE_PARAMETER, Integer.toString(nextPage))
                    .addQueryParameter(PER_PAGE_PARAMETER, Integer.toString(perPage))
                    .build()
                    .toString();

            String lastPageLink = server.url(path).newBuilder()
                    .addQueryParameter(PAGE_PARAMETER, Integer.toString(lastPage))
                    .addQueryParameter(PER_PAGE_PARAMETER, Integer.toString(perPage))
                    .build()
                    .toString();

            result.add(getLinkHeader(nextPageLink, NEXT_PAGE_REL));
            result.add(getLinkHeader(lastPageLink, LAST_PAGE_REL));
        }

        return result;
    }

    public static String getLinkHeader(String link, String rel) {
        return "<" + link + ">; rel=\"" + rel + "\"";
    }

}
