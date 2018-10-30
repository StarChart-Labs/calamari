/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.exception;

import org.starchartlabs.calamari.core.exception.RequestLimitExceededException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RequestLimitExceededExceptionTest {

    @Test
    public void constructNoCause() throws Exception {
        RequestLimitExceededException result = new RequestLimitExceededException("(limit)", "(reset)");

        Assert.assertEquals(result.getMessage(),
                "Maximum requests to GitHub exceeded. Limit of (limit), resets at (reset)");
        Assert.assertNull(result.getCause());
    }

    @Test
    public void constructWithCause() throws Exception {
        RuntimeException cause = new RuntimeException();
        RequestLimitExceededException result = new RequestLimitExceededException("(limit)", "(reset)", cause);

        Assert.assertEquals(result.getMessage(),
                "Maximum requests to GitHub exceeded. Limit of (limit), resets at (reset)");
        Assert.assertEquals(result.getCause(), cause);
    }

}
