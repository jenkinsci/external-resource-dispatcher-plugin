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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

/**
 * this is the util class for json rpc.
 * you can create different rpc client for use.
 * @author Leimeng Zhang
 *
 */
public final class JsonRpcUtil {

    /**
     * the default private constructor for utility class.
     */
    private JsonRpcUtil() {
    }

    /**
     * the logger.
     */
    private static final Logger logger = Logger.getLogger(JsonRpcUtil.class.getName());

    /**
     * create the Json RPC client using the specified url.
     * @param url the url of the RPC call.
     * @return the {@link JsonRpcHttpClient} to be used.
     */
    public static JsonRpcHttpClient createJsonRpcClient(String url) {
        JsonRpcHttpClient client = null;
        try {
            client = new JsonRpcHttpClient(new URL(url));
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not create the json rpc client because of malformed url: {0}",
                    url), e);
        }
        return client;
    }

    /**
     * create the Json RPC client using the specified url and a customized {@link ObjectMapper}.
     * @param url the url of the RPC call.
     * @param customizedObjectMapper the customized {@link ObjectMapper} to support rpc json format.
     * @return the {@link JsonRpcHttpClient} to be used.
     */
    public static JsonRpcHttpClient createJsonRpcClient(String url, ObjectMapper customizedObjectMapper) {
        JsonRpcHttpClient client = null;
        try {
            client = new JsonRpcHttpClient(customizedObjectMapper, new URL(url), new HashMap<String, String>());
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not create the json rpc client because of malformed url: {0}",
                    url), e);
        }
        return client;
    }
    /**
     * create the customized object mapper, which is used to read/write json
     * object. by doing so, the object[] will cast to be one jsonObject if
     * there is only one element inside. or it will become one jsonArray
     * which is expected to be jsonObject.
     * Eg. it will return {"resource":"12346579", "timeout":10} than
     * [{"resource":"12346579", "timeout":10}]
     *
     * @return the customized {@link ObjectMapper} which can help do the json rpc call.
     */
    public static ObjectMapper customizeObjectMapper() {
        ObjectMapper objMapper = new ObjectMapper() {
            /**
             * override to make it work as we expected for the one element object[].
             * @param params the object to be casted as JSON format.
             * @return {@link JsonNode} converted from the params.
             */
            @SuppressWarnings("unchecked")
            public JsonNode valueToTree(Object params) {
                if (params.getClass().isArray()) {
                    Object[] paramArray = (Object[])params;
                    if (paramArray.length == 1) { // if only one element  there.
                        return super.valueToTree(paramArray[0]);
                    }
                }
                return super.valueToTree(params);
            }
        };
        return objMapper;
    }
}
