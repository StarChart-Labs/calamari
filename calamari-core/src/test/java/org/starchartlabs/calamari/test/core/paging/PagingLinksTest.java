/*
 * Copyright (C) 2019 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.paging;

import java.util.Arrays;
import java.util.Optional;

import org.starchartlabs.calamari.core.paging.PagingLinks;
import org.starchartlabs.calamari.test.LinkHeaderTestSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PagingLinksTest {

    private static final String FIRST_PAGE_LINK = "https://api.github.com/user/repos?page=1&per_page=100";

    private static final String PREV_PAGE_LINK = "https://api.github.com/user/repos?page=2&per_page=100";

    private static final String NEXT_PAGE_LINK = "https://api.github.com/user/repos?page=4&per_page=100";

    private static final String LAST_PAGE_LINK = "https://api.github.com/user/repos?page=50&per_page=100";

    private static final String FIRST_PAGE_HEADER = LinkHeaderTestSupport.getLinkHeader(FIRST_PAGE_LINK, "first");

    private static final String PREV_PAGE_HEADER = LinkHeaderTestSupport.getLinkHeader(PREV_PAGE_LINK, "prev");

    private static final String NEXT_PAGE_HEADER = LinkHeaderTestSupport.getLinkHeader(NEXT_PAGE_LINK, "next");

    private static final String LAST_PAGE_HEADER = LinkHeaderTestSupport.getLinkHeader(LAST_PAGE_LINK, "last");

    private static final String MULTIPLE_HEADERS = NEXT_PAGE_HEADER + ", " + LAST_PAGE_HEADER;

    private static final String ALL_HEADERS = FIRST_PAGE_HEADER + ", " + PREV_PAGE_HEADER + ", " + NEXT_PAGE_HEADER
            + ", " + LAST_PAGE_HEADER;

    @Test
    public void firstPageOnly() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(FIRST_PAGE_HEADER));

        Assert.assertEquals(result.getFirstPageUrl().get(), FIRST_PAGE_LINK);
        Assert.assertFalse(result.getPreviousPageUrl().isPresent());
        Assert.assertFalse(result.getLastPageUrl().isPresent());
        Assert.assertFalse(result.getNextPageUrl().isPresent());
    }

    @Test
    public void prevPageOnly() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(PREV_PAGE_HEADER));

        Assert.assertFalse(result.getFirstPageUrl().isPresent());
        Assert.assertEquals(result.getPreviousPageUrl().get(), PREV_PAGE_LINK);
        Assert.assertFalse(result.getLastPageUrl().isPresent());
        Assert.assertFalse(result.getNextPageUrl().isPresent());
    }

    @Test
    public void nextPageOnly() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(NEXT_PAGE_HEADER));

        Assert.assertFalse(result.getFirstPageUrl().isPresent());
        Assert.assertFalse(result.getPreviousPageUrl().isPresent());
        Assert.assertFalse(result.getLastPageUrl().isPresent());
        Assert.assertEquals(result.getNextPageUrl().get(), NEXT_PAGE_LINK);
    }

    @Test
    public void lastPageOnly() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(LAST_PAGE_HEADER));

        Assert.assertFalse(result.getFirstPageUrl().isPresent());
        Assert.assertFalse(result.getPreviousPageUrl().isPresent());
        Assert.assertFalse(result.getNextPageUrl().isPresent());
        Assert.assertEquals(result.getLastPageUrl().get(), LAST_PAGE_LINK);
    }

    @Test
    public void validLinkHeadersSingleEntry() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(MULTIPLE_HEADERS));

        Assert.assertFalse(result.getFirstPageUrl().isPresent());
        Assert.assertFalse(result.getPreviousPageUrl().isPresent());
        Assert.assertEquals(result.getNextPageUrl().get(), NEXT_PAGE_LINK);
        Assert.assertEquals(result.getLastPageUrl().get(), LAST_PAGE_LINK);
    }

    @Test
    public void validLinkHeadersMultiEntry() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(NEXT_PAGE_HEADER, LAST_PAGE_HEADER));

        Assert.assertFalse(result.getFirstPageUrl().isPresent());
        Assert.assertFalse(result.getPreviousPageUrl().isPresent());
        Assert.assertEquals(result.getNextPageUrl().get(), NEXT_PAGE_LINK);
        Assert.assertEquals(result.getLastPageUrl().get(), LAST_PAGE_LINK);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getPageNullUrl() throws Exception {
        PagingLinks.getPage(null);
    }

    @Test
    public void getPageNoParameter() throws Exception {
        Optional<Integer> result = PagingLinks.getPage("https://api.github.com/user/repos");

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void getPageNonNumericValue() throws Exception {
        Optional<Integer> result = PagingLinks.getPage("https://api.github.com/user/repos?page=blah");

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void getPage() throws Exception {
        Optional<Integer> result = PagingLinks.getPage("https://api.github.com/user/repos?page=50");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().intValue(), 50);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getPerPageNullUrl() throws Exception {
        PagingLinks.getPerPage(null);
    }

    @Test
    public void getPerPageNoParameter() throws Exception {
        Optional<Integer> result = PagingLinks.getPerPage("https://api.github.com/user/repos");

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void getPerPageNonNumericValue() throws Exception {
        Optional<Integer> result = PagingLinks.getPerPage("https://api.github.com/user/repos?per_page=blah");

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void getPerPage() throws Exception {
        Optional<Integer> result = PagingLinks.getPerPage("https://api.github.com/user/repos?per_page=50");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().intValue(), 50);
    }

    @Test
    public void hashCodeEqualWhenDataEqual() throws Exception {
        PagingLinks result1 = new PagingLinks(Arrays.asList(ALL_HEADERS));
        PagingLinks result2 = new PagingLinks(Arrays.asList(ALL_HEADERS));

        Assert.assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    public void equalsNull() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(ALL_HEADERS));

        Assert.assertFalse(result.equals(null));
    }

    // Suppress unlikely argument type, as this test is specifically to validate behavior in that case
    @Test
    @SuppressWarnings("unlikely-arg-type")
    public void equalsDifferentClass() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(ALL_HEADERS));

        Assert.assertFalse(result.equals("string"));
    }

    @Test
    public void equalsSelf() throws Exception {
        PagingLinks result = new PagingLinks(Arrays.asList(ALL_HEADERS));

        Assert.assertTrue(result.equals(result));
    }

    @Test
    public void equalsDifferentData() throws Exception {
        PagingLinks result1 = new PagingLinks(Arrays.asList(ALL_HEADERS));
        PagingLinks result2 = new PagingLinks(Arrays.asList(MULTIPLE_HEADERS));

        Assert.assertFalse(result1.equals(result2));
    }

    @Test
    public void equalsSameData() throws Exception {
        PagingLinks result1 = new PagingLinks(Arrays.asList(ALL_HEADERS));
        PagingLinks result2 = new PagingLinks(Arrays.asList(ALL_HEADERS));

        Assert.assertTrue(result1.equals(result2));
    }

}
