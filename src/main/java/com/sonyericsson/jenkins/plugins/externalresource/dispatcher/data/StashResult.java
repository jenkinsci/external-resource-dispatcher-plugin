/*
 *  The MIT License
 *
 *  Copyright 2011 Sony Ericsson Mobile Communications. All rights reserved.
 *  Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.ExternalResourceManager}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class StashResult implements Serializable {

    private int errorCode;
    private String message;
    private String key;
    private Status status;
    private Lease lease;

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
     * @param key             the key to use for performing a release of the resource.
     */
    public StashResult(String message, String key) {
        this.errorCode = 0;
        this.status = Status.OK;
        this.message = message;
        this.key = key;
    }

    /**
     * standard constructor.
     *
     * @param errorCode       protocol specific code of the result. 0 indicating OK.
     * @param message         a message from the service.
     * @param key             the key to use for performing a release of the resource.
     * @param status          the status of the call.
     * @param lease           when will it end.
     */
    public StashResult(int errorCode, String message, String key, Status status, Lease lease) {
        this.errorCode = errorCode;
        this.message = message;
        this.key = key;
        this.status = status;
        this.lease = lease;
    }

    /**
     * If the response is an OK or a not OK response.
     *
     * @return true if all is OK.
     */
    public boolean isOk() {
        return status == Status.OK;
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
     * The key for future locking/releasing activities.
     *
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * the lease when it is end.
     * @return {@link Lease} when it is end.
     */
    public Lease getLease() {
        return lease;
    }

    /**
     * the status of the call.
     * @return the status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * the status of the Stash Result.
     * @author Zhang Leimeng
     */
    public static enum Status {
        /**
         * the result status is OK.
         */
        OK,
        /**
         * the result status is error.
         * check the error code in this case.
         */
        NO
    }
}
