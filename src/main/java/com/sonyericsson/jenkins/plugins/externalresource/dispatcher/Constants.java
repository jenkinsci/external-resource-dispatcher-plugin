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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher;

/**
 * Common constants.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public final class Constants {
    /**
     * Serialization alias (JSON and XStream) for
     * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource}.
     */
    public static final String SERIALIZATION_ALIAS_EXTERNAL_RESOURCE = "external-resource";
    /**
     * Separator for string device selection.
     */
    public static final String STRING_DEVICE_SELECTION_SEPARATOR = ".";
    /**
     * Separator for string device selection with escape.
     */
    public static final String STRING_DEVICE_SELECTION_SEPARATOR_WITH_ESCAPE = "\\.";


    /**
     * Id attribute in a JSON object.
     */
    public static final String JSON_ATTR_ID = "id";

    /**
     * Reserved attribute in a JSON object.
     */
    public static final String JSON_ATTR_RESERVED = "reserved";

    /**
     * Locked attribute in a JSON object.
     */
    public static final String JSON_ATTR_LOCKED = "locked";

    /**
     * The root URL ror http commands relating to external resource.
     */
    public static final String EXTERNAL_RESOURCE_HTTP_COMMANDS_URL = "external-resource-httpcli";

    /**
     * The JSON attribute for
     * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource#setEnabled(boolean)}.
     */
    public static final String JSON_ATTR_ENABLED = "enabled";

    /**
     * The prio order for {@link ExternalResourceQueueTaskDispatcher} so that other
     * {@link hudson.model.queue.QueueTaskDispatcher}s can make sure to run before or after this one.
     */
    public static final int QTD_ORDINAL = 5;

    /**
     * Default number of seconds to reserve a resource.
     */
    public static final int DEFAULT_RESERVE_TIME = 3; //TODO probably needs tweaking.
    /**
     * JSON Attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo#stashedBy}.
     */
    public static final String JSON_ATTR_STASHED_BY = "stashed-by";
    /**
     * JSON Attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo#type}.
     */
    public static final String JSON_ATTR_TYPE = "type";
    /**
     * JSON Attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo#lease}.
     */
    public static final String JSON_ATTR_LEASE = "lease";
    /**
     * JSON attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo#key}.
     */
    public static final String JSON_ATTR_KEY = "key";
    /**
     * JSON attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.Lease#serverTime}.
     */
    public static final String JSON_ATTR_TIME_MILLIS = "millis";
    /**
     * JSON attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.Lease#serverTime}.
     */
    public static final String JSON_ATTR_TIME_TIME_ZONE = "time-zone";
    /**
     * JSON attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.Lease#serverTime}.
     */
    public static final String JSON_ATTR_SERVER_TIME = "server-time";
    /**
     * JSON attribute for {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.Lease#slaveIsoTime}.
     */
    public static final String JSON_ATTR_SLAVE_ISO_TIME = "slave-iso-time";


    /**
     * The path to the parent node where the locked external resource should be placed.
     */
    public static final String[] BUILD_LOCKED_RESOURCE_PARENT_PATH = {"external-resources"};

    /**
     * The name of the locked external resource when it is placed in its parent node.
     */
    public static final String BUILD_LOCKED_RESOURCE_NAME = "locked";

    /**
     * The full path to the locked resource for a buid.
     * Constructed by {@link #BUILD_LOCKED_RESOURCE_PARENT_PATH} + {@link #BUILD_LOCKED_RESOURCE_NAME}.
     */
    public static final String[] BUILD_LOCKED_RESOURCE_PATH;

    static {
        BUILD_LOCKED_RESOURCE_PATH = new String[BUILD_LOCKED_RESOURCE_PARENT_PATH.length + 1];
        System.arraycopy(BUILD_LOCKED_RESOURCE_PARENT_PATH, 0,
                BUILD_LOCKED_RESOURCE_PATH, 0, BUILD_LOCKED_RESOURCE_PARENT_PATH.length);
        BUILD_LOCKED_RESOURCE_PATH[BUILD_LOCKED_RESOURCE_PARENT_PATH.length] =
                BUILD_LOCKED_RESOURCE_NAME;
    }

    /**
     * Utility Constructor.
     */
    private Constants() {
    }
}
