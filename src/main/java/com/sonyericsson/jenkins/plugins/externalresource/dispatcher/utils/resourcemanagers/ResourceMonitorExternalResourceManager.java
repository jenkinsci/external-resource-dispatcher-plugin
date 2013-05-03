/*
 *  The MIT License
 *
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

package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.Lease;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.JsonRpcUtil;
import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Node;
import net.sf.json.JSONObject;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager that communicates via JSON-RPC to the External Resource Monitor.
 *
 * @author Robert Sandell &lt;robert.sandell@sonymobile.com&gt;
 */
@Extension
public class ResourceMonitorExternalResourceManager extends ExternalResourceManager {

    /**
     * the logger.
     */
    private static final Logger logger = Logger.getLogger(ResourceMonitorExternalResourceManager.class.getName());

    /**
     * the method name of reserve.
     */
    private static final String RESERVE_METHOD = "ResourceMonitor.Resources.Reserve";

    /**
     * the method of lock.
     */
    private static final String LOCK_METHOD = "ResourceMonitor.Resources.Lock";

    /**
     * the method of release.
     */
    private static final String RELEASE_METHOD = "ResourceMonitor.Resources.Release";

    /**
     * the http url template of the RPC call. 0: the host name. 1: the port. 2: the suffix if existed.
     */
    private static final String RPC_CALL_URL_TEMPLATE = "http://{0}:{1}/{2}";

    /**
     * the key of the resource parameter in sent json.
     */
    private static final String RESOURCE = "resource";

    /**
     * the key of the reserve key parameter in sent json.
     */
    private static final String RESERVE_KEY = "key";

    /**
     * the key of the time parameter in sent json.
     */
    private static final String TIMEOUT = "timeout";

    /**
     * the key of the clientInfo parameter in sent json.
     */
    private static final String CLIENT_INFO = "clientInfo";

    /**
     * the key of the id parameter in sent json.
     */
    private static final String ID = "id";

    /**
     * the key of the url parameter in sent json.
     */
    private static final String URL = "url";

    /**
     * the default port the rpc call.
     */
    private static final String PORT = "8080";

    /**
     * get the accessible address on the {@link hudson.model.Node}.
     *
     * @param node the specified {@link hudson.model.Node} we will connect.
     * @return the hostName of the node.
     * @throws InterruptedException when {@link InterruptedException} happened when get host name.
     * @throws java.io.IOException  when {@link java.io.IOException} happened when get host name.
     */
    private String getURL(Node node) throws IOException, InterruptedException {
        String nodeURL = null;
        if (null != node) {
            String hostName = node.toComputer().getHostName();
            if (null != hostName) {
                // TODO: hard code the port and suffix here. need a configure page to hold it somewhere later.
                nodeURL = MessageFormat.format(RPC_CALL_URL_TEMPLATE, hostName, PORT, "");
            }
        }
        return nodeURL;
    }

    /**
     * get the resource id from the resource.
     *
     * @param resource the external resource supposed to be working on.
     * @return the specified resource id.
     */
    private String getResourceId(ExternalResource resource) {
        return resource.getId();
    }

    @Override
    public String getDisplayName() {
        return Messages.ResourceMonitorExternalResourceManager_DisplayName();
    }

    @Override
    public StashResult doReserve(Node node, ExternalResource resource, int seconds, String reservedBy) {
        RpcResult rpcRes = null;
        String resourceId = getResourceId(resource);
        try {
            JsonRpcHttpClient client = JsonRpcUtil.createJsonRpcClient(getURL(node),
                    JsonRpcUtil.customizeObjectMapper());
            if (null != client && null != resourceId) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put(RESOURCE, resourceId);
                params.put(TIMEOUT, seconds);
                JSONObject clientInfo = new JSONObject();
                clientInfo.put(ID, Hudson.getInstance().getRootUrl());
                clientInfo.put(URL, reservedBy);
                params.put(CLIENT_INFO, clientInfo);
                logger.log(Level.FINE, "reserving: {0}", params.toString());
                rpcRes = (RpcResult)client.invoke(RESERVE_METHOD, new Object[]{params}, RpcResult.class);
            }
        } catch (JsonGenerationException jge) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not reserve the resource {0} invalid json generated to rpc call.",
                    resourceId), jge);
        } catch (JsonMappingException jme) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not reserve the resource {0} invalid json mapping.",
                    resourceId), jme);
        } catch (JsonParseException jpre) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not reserve the resource {0} failed to parse json.",
                    resourceId), jpre);
        } catch (JsonProcessingException jpoe) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not reserve the resource {0} failed to process json.",
                    resourceId), jpoe);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not reserve the resource {0}.",
                    resourceId), ioe);
        } catch (Error e) {
            // if error type, throw it.
            throw e;
        } catch (Throwable e) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not reserve the resource {0}.",
                    resourceId), e);
        }
        return convert(rpcRes);
    }

    @Override
    public StashResult doLock(Node node, ExternalResource resource, String key, String lockedBy) {
        RpcResult rpcRes = null;
        String resourceId = getResourceId(resource);

        try {
            JsonRpcHttpClient client = JsonRpcUtil.createJsonRpcClient(getURL(node),
                    JsonRpcUtil.customizeObjectMapper());
            if (null != client && null != resourceId) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put(RESOURCE, resourceId);
                params.put(RESERVE_KEY, key);
                JSONObject clientInfo = new JSONObject();
                clientInfo.put(ID, Hudson.getInstance().getRootUrl());
                clientInfo.put(URL, lockedBy);
                params.put(CLIENT_INFO, clientInfo);
                logger.log(Level.FINE, "locking: {0}", params.toString());
                rpcRes = (RpcResult)client.invoke(LOCK_METHOD, new Object[]{params}, RpcResult.class);
            }
        } catch (JsonGenerationException jge) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not lock the resource {0} invalid json generated to rpc call.",
                    resourceId), jge);
        } catch (JsonMappingException jme) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not lock the resource {0} invalid json mapping.",
                    resourceId), jme);
        } catch (JsonParseException jpre) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not lock the resource {0} failed to parse json.",
                    resourceId), jpre);
        } catch (JsonProcessingException jpoe) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not lock the resource {0} failed to process json.",
                    resourceId), jpoe);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not lock the resource {0}.",
                    resourceId), ioe);
        } catch (Error e) {
            // if error type, throw it.
            throw e;
        } catch (Throwable e) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not lock the resource {0}.",
                    resourceId), e);
        }
        // FIX the issue , missing key when release. because the reservekey is not
        // returned by the lock call. have to give the value here.
        // Reuse the previous one if null is returned.
        if (rpcRes != null && rpcRes.getKey() == null) {
            rpcRes.setKey(key);
        }
        return convert(rpcRes);
    }

    @Override
    public StashResult doRelease(Node node, ExternalResource resource, String key, String releasedBy) {
        RpcResult rpcRes = null;
        String resourceId = getResourceId(resource);

        try {
            JsonRpcHttpClient client = JsonRpcUtil.createJsonRpcClient(getURL(node));
            if (null != client && null != resourceId) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put(RESOURCE, resourceId);
                params.put(RESERVE_KEY, key);
                JSONObject clientInfo = new JSONObject();
                clientInfo.put(ID, Hudson.getInstance().getRootUrl());
                clientInfo.put(URL, releasedBy);
                params.put(CLIENT_INFO, clientInfo);
                logger.log(Level.FINE, "releasing: {0}", params.toString());
                rpcRes = (RpcResult)client.invoke(RELEASE_METHOD, new Object[]{params}, RpcResult.class);
            }
        } catch (JsonGenerationException jge) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not release the resource {0} invalid json generated to rpc call.",
                    resourceId), jge);
        } catch (JsonMappingException jme) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not release the resource {0} invalid json mapping.",
                    resourceId), jme);
        } catch (JsonParseException jpre) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not release the resource {0} failed to parse json.",
                    resourceId), jpre);
        } catch (JsonProcessingException jpoe) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not release the resource {0} failed to process json.",
                    resourceId), jpoe);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, MessageFormat.format(
                    "Can not release the resource {0}.",
                    resourceId), ioe);
        } catch (Error e) {
            // if error type, throw it.
            throw e;
        } catch (Throwable e) {
            logger.log(Level.WARNING, MessageFormat.format("Can not release the resource {0}.", resourceId), e);
        }
        return convert(rpcRes);
    }

    @Override
    public boolean isExternalLockingOk() {
        return true;
    }

    @Override
    public void updateMetadata(AbstractMetadataValue value) {
        //placeholder for when the update Metadata method is available in the ResourceMonitor.
    }

    /**
     * convert from the {@link RpcResult} to the {@link StashResult}.
     *
     * @param rpcResult the specified {@link RpcResult}
     * @return the {@link StashResult}
     */
    private StashResult convert(RpcResult rpcResult) {
        StashResult targetResult = null;
        Lease lease = null;
        if (null != rpcResult) {
            lease = Lease.createInstance(rpcResult.getTime(), rpcResult.getTimezone(), rpcResult.getIsotime());
            targetResult = new StashResult(rpcResult.getCode(), rpcResult.getMessage(),
                    rpcResult.getKey(), rpcResult.getStatus(), lease);
        }
        return targetResult;
    }

    /**
     * this is the rpc call result class.
     *
     * @author Leimeng Zhang
     */
    static class RpcResult {
        private StashResult.Status status;
        private String message;
        private int code;
        private String key;
        private int timezone;
        private long time;
        private String isotime;

        /**
         * the rpc call status.
         *
         * @return the status of the rpc call. possible values are : OK and NO.
         */
        public StashResult.Status getStatus() {
            return status;
        }

        /**
         * set value for the response status.
         *
         * @param status the status of response.
         */
        public void setStatus(StashResult.Status status) {
            this.status = status;
        }

        /**
         * the reserved key returned by the reserve call.
         *
         * @return the reserved key which can used to lock a resource.
         */
        public String getKey() {
            return key;
        }

        /**
         * set value for the reserved key.
         *
         * @param key the reserved key of response.
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * the status code specified by the protocol.
         *
         * @return the status code of response.
         */
        public int getCode() {
            return code;
        }

        /**
         * set value for the status code.
         *
         * @param code the call status.
         */
        public void setCode(int code) {
            this.code = code;
        }

        /**
         * the message of response.
         *
         * @return the message returned from call.
         */
        public String getMessage() {
            return message;
        }

        /**
         * set message of the response.
         *
         * @param message the message of the response.
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * the offset of local timezone.
         *
         * @return the offset of local timezone.
         */
        public int getTimezone() {
            return timezone;
        }

        /**
         * set the timezone.
         *
         * @param timezone timezone.
         */
        public void setTimezone(int timezone) {
            this.timezone = timezone;
        }

        /**
         * the time in ms unit.
         *
         * @return the time in ms unit.
         */
        public long getTime() {
            return time;
        }

        /**
         * set the time.
         *
         * @param time the time in ms unit.
         */
        public void setTime(long time) {
            this.time = time;
        }

        /**
         * the iso time of the time value.
         *
         * @return the iso time of the time.
         */
        public String getIsotime() {
            return isotime;
        }

        /**
         * set the iso time.
         *
         * @param isotime the iso time of the time.
         */
        public void setIsotime(String isotime) {
            this.isotime = isotime;
        }
    }
}
