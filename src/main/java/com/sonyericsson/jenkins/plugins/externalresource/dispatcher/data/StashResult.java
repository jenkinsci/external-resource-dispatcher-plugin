/*
 *  The MIT License
 *
 *  Copyright 2011 Sony Ericsson Mobile Communications. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data;

import java.io.Serializable;

/**
 * Lock and reservation result from an
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class StashResult implements Serializable {

    /**
     * Status code indicating an OK result.
     */
    public static final int STATUS_OK = 0;

    private int errorCode;
    private String message;
    private long leaseExpireTime;
    private String key;

    /**
     * Default Constructor.
     */
    public StashResult() {
    }

    /**
     * Constructor for an error result.
     *
     * @param errorCode the error code
     * @param message   the error message.
     */
    public StashResult(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * Constructor for an OK result.
     *
     * @param message         the message
     * @param leaseExpireTime when the lease expires (unix tics)
     * @param key             the key to use for performing a release of the resource.
     */
    public StashResult(String message, long leaseExpireTime, String key) {
        this.errorCode = 0;
        this.message = message;
        this.leaseExpireTime = leaseExpireTime;
        this.key = key;
    }

    /**
     * standard constructor.
     *
     * @param errorCode       protocol specific code of the result. 0 indicating OK.
     * @param message         a message from the service.
     * @param leaseExpireTime when the lease expires (unix tics)
     * @param key             the key to use for performing a release of the resource.
     */
    public StashResult(int errorCode, String message, long leaseExpireTime, String key) {
        this.errorCode = errorCode;
        this.message = message;
        this.leaseExpireTime = leaseExpireTime;
        this.key = key;
    }

    /**
     * If the response is an OK or a not OK response.
     *
     * @return true if all is OK.
     */
    public boolean isOk() {
        return errorCode == STATUS_OK;
    }

    /**
     * The protocol specific error code.
     *
     * @return the status of the response.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * The message from the service.
     *
     * @return human readable status.
     */
    public String getMessage() {
        return message;
    }

    /**
     * The unix time when the resource gets released.
     *
     * @return time since the epoch.
     */
    public long getLeaseExpireTime() {
        return leaseExpireTime;
    }

    /**
     * The key for future locking/releasing activities.
     *
     * @return the key.
     */
    public String getKey() {
        return key;
    }
}
