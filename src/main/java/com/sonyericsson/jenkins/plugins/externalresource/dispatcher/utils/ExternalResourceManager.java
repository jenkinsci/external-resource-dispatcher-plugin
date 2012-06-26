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

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.cli.ErCliUtils;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.Lease;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult.Status;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.triggers.Trigger;
import net.sf.json.JSONObject;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for handling reservation of resources by external services. For example the external resources on a slave
 * might be managed by a daemon who handles locking and unlocking of the resources on the slave itself, extend this to
 * provide your own implementation of such a communication interface.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public abstract class ExternalResourceManager implements ExtensionPoint {

    /**
     * The name of the manager to show the admin.
     *
     * @return the name.
     */
    public abstract String getDisplayName();

    /**
     * Reserve the resource on the node. A reservation has a deadline, if the device isn't locked until the lease
     * expires the service should unlock the resource so it can be used by another build. So if the {@link
     * com.sonyericsson.jenkins.plugins.externalresource.dispatcher.ExternalResourceQueueTaskDispatcher} reserves a
     * device but another {@link hudson.model.queue.QueueTaskDispatcher} vetoes the build, the device should not be set
     * to used for too long.
     *
     * @param node     the node to communicate with.
     * @param resource the resource to reserve.
     * @param seconds  the number of seconds the lease should be.
     * @param reservedBy a String describing what reserved the resource.
     * @return the result.
     */
    public abstract StashResult reserve(Node node, ExternalResource resource, int seconds, String reservedBy);

    /**
     * Locks the resource (permanently) until it is unlocked, no other build should be able to use this resource.
     *
     * @param node     the node holding the resource.
     * @param resource the resource to lock.
     * @param key      the key to be able to lock it (retained from
     *                 {@link #reserve(hudson.model.Node, ExternalResource, int, String)}).
     * @param lockedBy a String describing what locked the resource.
     * @return the result.
     */
    public abstract StashResult lock(Node node, ExternalResource resource, String key, String lockedBy);

    /**
     * Releases the resource, other builds can now use it.
     *
     * @param node     the node holding the resource.
     * @param resource the resource to unlock.
     * @param key      the key to unlock the resource with (retained from a previous call to
     *                 {@link  #lock(hudson.model.Node,
     *                 com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource,
     *                 String, String)}.
     * @param releasedBy a String describing what released the resource.
     * @return the result.
     */
    public abstract StashResult release(Node node, ExternalResource resource, String key, String releasedBy);

    /**
     * Answers true if these operations are allowed using this ExternalResourceManager.
     * @return true if allowed, false if not.
     */
    public abstract boolean isExternalLockingOk();
    /**
     * A manager that does nothing.
     */
    @Extension
    public static class NoopExternalResourceManager extends ExternalResourceManager {

        @Override
        public String getDisplayName() {
            return Messages.NoopExternalResourceManager_DisplayName();
        }

        /**
         * The result that will be returned for every operation.
         */
        protected final StashResult okResult = new StashResult("noop", "noop");

        @Override
        public StashResult reserve(Node node, ExternalResource resource, int seconds, String reservedBy) {
            Trigger.timer.schedule(new ReservationTimeoutTask(node.getNodeName(), resource.getId()),
                    TimeUnit.SECONDS.toMillis(seconds));
            return okResult;
        }

        @Override
        public StashResult lock(Node node, ExternalResource resource, String key, String lockedBy) {
            return okResult;
        }

        @Override
        public StashResult release(Node node, ExternalResource resource, String key, String releasedBy) {
            return okResult;
        }

        @Override
        public boolean isExternalLockingOk() {
            return false;
        }

        /**
         * TimerTask to schedule when a Noop reservation times out.
         */
        static class ReservationTimeoutTask extends TimerTask {
            /**
             * The logger
             */
            private static final Logger logger = Logger.getLogger(ReservationTimeoutTask.class.getName());
            private String node;
            private String id;

            /**
             * standard Constructor.
             * @param node the name of the node.
             * @param id the id of the resource.
             */
            ReservationTimeoutTask(String node, String id) {
                this.node = node;
                this.id = id;
            }

            @Override
            public void run() {
                try {
                    logger.fine("Reservation timeout.");
                    ExternalResource er = ErCliUtils.findExternalResource(node, id);
                    er.doExpireReservation();
                } catch (CmdLineException e) {
                    logger.log(Level.WARNING, "Failed to timeout a reservation of " + id + " on node " + node + "!", e);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to save the new reservation state to disk!", e);
                }
            }
        }
    }

    /**
     * A manager that communicated via JSON-RPC to the SEMC Device Monitor.
     */
    @Extension
    public static class DeviceMonitorExternalResourceManager extends ExternalResourceManager {

        /**
         * the logger.
         */
        private static final Logger logger = Logger.getLogger(DeviceMonitorExternalResourceManager.class.getName());

        /**
         * the method name of reserve.
         */
        private static final String RESERVE_METHOD = "DeviceMonitor.Devices.Reserve";

        /**
         * the method of lock.
         */
        private static final String LOCK_METHOD = "DeviceMonitor.Devices.Lock";

        /**
         * the method of release.
         */
        private static final String RELEASE_METHOD = "DeviceMonitor.Devices.Release";

        /**
         * the http url template of the RPC call. 0: the host name. 1: the port. 2: the suffix if existed.
         */
        private static final String RPC_CALL_URL_TEMPLATE = "http://{0}:{1}/{2}";

        /**
         * the key of the device parameter in sent json.
         */
        private static final String DEVICE = "device";

        /**
         * the key of the reservekey parameter in sent json.
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
         * get the accessible address on the {@link Node}.
         *
         * @param node the specified {@link Node} we will connect.
         * @return the hostName of the node.
         *
         * @throws InterruptedException when {@link InterruptedException} happened when get host name.
         * @throws IOException          when {@link IOException} happened when get host name.
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
         * get the device id from the resource.
         *
         * @param resource the external resource supposed to be working on.
         * @return the specified deviceId.
         */
        private String getDeviceId(ExternalResource resource) {
            return resource.getId();
        }

        @Override
        public String getDisplayName() {
            return Messages.DeviceMonitorExternalResourceManager_DisplayName();
        }

        @Override
        public StashResult reserve(Node node, ExternalResource resource, int seconds, String reservedBy) {
            RpcResult rpcRes = null;
            String deviceId = getDeviceId(resource);
            try {
                JsonRpcHttpClient client = JsonRpcUtil.createJsonRpcClient(getURL(node),
                        JsonRpcUtil.customizeObjectMapper());
                if (null != client && null != deviceId) {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put(DEVICE, deviceId);
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
                        "Can not reserve the device {0} because of the invalid json generated to rpc call.",
                        deviceId), jge);
            } catch (JsonMappingException jme) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not reserve the device {0} because of the invalid json mapping.",
                        deviceId), jme);
            } catch (JsonParseException jpre) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not reserve the device {0} because failed to parse json.",
                        deviceId), jpre);
            } catch (JsonProcessingException jpoe) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not reserve the device {0} because failed to process json.",
                        deviceId), jpoe);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not reserve the device {0} because IO exception happened.",
                        deviceId), ioe);
            } catch (Error e) {
                // if error type, throw it.
                throw e;
            } catch (Throwable e) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not reserve the device {0}.",
                        deviceId), e);
            }
            return convert(rpcRes);
        }

        @Override
        public StashResult lock(Node node, ExternalResource resource, String key, String lockedBy) {
            RpcResult rpcRes = null;
            String deviceId = getDeviceId(resource);

            try {
                JsonRpcHttpClient client = JsonRpcUtil.createJsonRpcClient(getURL(node),
                        JsonRpcUtil.customizeObjectMapper());
                if (null != client && null != deviceId) {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put(DEVICE, deviceId);
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
                        "Can not lock the device {0} because of the invalid json generated to rpc call.",
                        deviceId), jge);
            } catch (JsonMappingException jme) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not lock the device {0} because of the invalid json mapping.",
                        deviceId), jme);
            } catch (JsonParseException jpre) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not lock the device {0} because failed to parse json.",
                        deviceId), jpre);
            } catch (JsonProcessingException jpoe) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not lock the device {0} because failed to process json.",
                        deviceId), jpoe);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not lock the device {0} because IO exception happened.",
                        deviceId), ioe);
            } catch (Error e) {
                // if error type, throw it.
                throw e;
            } catch (Throwable e) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not lock the device {0}.",
                        deviceId), e);
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
        public StashResult release(Node node, ExternalResource resource, String key, String releasedBy) {
            RpcResult rpcRes = null;
            String deviceId = getDeviceId(resource);

            try {
                JsonRpcHttpClient client = JsonRpcUtil.createJsonRpcClient(getURL(node));
                if (null != client && null != deviceId) {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put(DEVICE, deviceId);
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
                        "Can not release the device {0} because of the invalid json generated to rpc call.",
                        deviceId), jge);
            } catch (JsonMappingException jme) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not release the device {0} because of the invalid json mapping.",
                        deviceId), jme);
            } catch (JsonParseException jpre) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not release the device {0} because failed to parse json.",
                        deviceId), jpre);
            } catch (JsonProcessingException jpoe) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not release the device {0} because failed to process json.",
                        deviceId), jpoe);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, MessageFormat.format(
                        "Can not release the device {0} because IO exception happened.",
                        deviceId), ioe);
            } catch (Error e) {
                // if error type, throw it.
                throw e;
            } catch (Throwable e) {
                logger.log(Level.WARNING, MessageFormat.format("Can not release the device {0}.", deviceId), e);
            }
            return convert(rpcRes);
        }

        @Override
        public boolean isExternalLockingOk() {
            return true;
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
            private Status status;
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
            public Status getStatus() {
                return status;
            }

            /**
             * set value for the response status.
             *
             * @param status the status of response.
             */
            public void setStatus(Status status) {
                this.status = status;
            }

            /**
             * the reserved key returned by the reserve call.
             *
             * @return the reserved key which can used to lock a device.
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
}
