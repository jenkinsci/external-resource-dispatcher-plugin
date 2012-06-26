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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.cli;

import com.sonyericsson.hudson.plugins.metadata.cli.CliResponse;
import com.sonyericsson.hudson.plugins.metadata.cli.CliUtils;
import com.sonyericsson.hudson.plugins.metadata.cli.CliResponse.Type;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter;
import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.RootAction;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sonyericsson.hudson.plugins.metadata.cli.CliResponse.sendOk;
import static com.sonyericsson.hudson.plugins.metadata.cli.CliResponse.sendResponse;

/**
 * Http interface for the CLI commands.
 * <p/>
 * As some systems prefer to have a bit more intimate call API towards other systems than what {@link
 * hudson.cli.CLICommand}s provide. This action exposes {@link ExternalResource#doEnable(boolean)}
 * to a standard HTTP post or GET.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension
public class ExternalResourceHttpCommands implements RootAction {

    private static final Logger logger = Logger.getLogger(ExternalResourceHttpCommands.class.getName());

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return Constants.EXTERNAL_RESOURCE_HTTP_COMMANDS_URL;
    }

    /**
     * Enables an {@link ExternalResource} with the id on a given node. The JSON output is similar to the metadata
     * commands.
     *
     * @param node     the node where the resource is located.
     * @param id       the id of the resource.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see CliResponse
     */
    @SuppressWarnings("unused")
    public void doEnable(
            @QueryParameter(value = "node", required = true) final String node,
            @QueryParameter(value = "id", required = true) final String id,
            StaplerResponse response) throws IOException {
        doEnableDisable(node, id, true, response);
    }

    /**
     * Disables an {@link ExternalResource} with the id on a given node. The JSON output is similar to the metadata
     * commands.
     *
     * @param node     the node where the resource is located.
     * @param id       the id of the resource.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see CliResponse
     */
    @SuppressWarnings("unused")
    public void doDisable(
            @QueryParameter(value = "node", required = true) final String node,
            @QueryParameter(value = "id", required = true) final String id,
            StaplerResponse response) throws IOException {
        doEnableDisable(node, id, false, response);
    }

    /**
     * Does the enable/disable.
     *
     * @param node the node where the resource is located.
     * @param id the id of the resource.
     * @param enable true to enable, false to disable.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see #doEnable(String, String, org.kohsuke.stapler.StaplerResponse)
     * @see #doDisable(String, String, org.kohsuke.stapler.StaplerResponse)
     * @see ExternalResource#doEnable(boolean)
     */
    private void doEnableDisable(final String node, final String id, final boolean enable, StaplerResponse response)
            throws IOException {
        Something something = new Something() {
            @Override
            public void doIt(ExternalResource resource, StaplerResponse response) throws IOException {
                try {
                    resource.doEnable(enable);
                    sendOk(response);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Failed to save the changes to disk, but the resource state has changed.", response);
                }
            }
        };
        doSomething(node, id, something, response);
    }

    /**
     * Make an {@link ExternalResource} reservation expired with the id on a given node. The JSON output is similar to
     * the metadata commands.
     *
     * @param node     the node where the resource is located.
     * @param id       the id of the resource.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see CliResponse
     */
    @SuppressWarnings("unused")
    public void doExpireReservation(
            @QueryParameter(value = "node", required = true) final String node,
            @QueryParameter(value = "id", required = true) final String id,
        StaplerResponse response) throws IOException {
        Something something = new Something() {
            @Override
            public void doIt(ExternalResource resource, StaplerResponse response) throws IOException {
                try {
                    resource.doExpireReservation();
                    sendOk(response);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Failed to save the changes to disk, but the resource state has changed.", response);
                }
            }
        };
        doSomething(node, id, something, response);
    }

    /**
     * Signal from an external lock handler that an {@link ExternalResource} has been locked.
     *
     * @param node     the node where the resource is located.
     * @param id       the id of the resource.
     * @param response the response handle to write to.
     * @param lockedBy a String describing what has locked the resource.
     * @param clientInfo the information about the client that called this.
     * @throws IOException if so.
     * @see CliResponse
     */
    @SuppressWarnings("unused")
    public void doLockResource(
            @QueryParameter(value = "node", required = true) final String node,
            @QueryParameter(value = "id", required = true) final String id,
            @QueryParameter(value = "lockedBy", required = true) final String lockedBy,
            @QueryParameter(value = "clientInfo", required = true) final String clientInfo,
        StaplerResponse response) throws IOException {
        Something something = new Something() {
            @Override
            public void doIt(ExternalResource resource, StaplerResponse response) throws IOException {
                try {
                    if (ErCliUtils.isRequestCircular(clientInfo)) {
                        logger.log(Level.FINE, "Request was circular for: {0}", clientInfo);
                        return;
                    }
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Locking resource: {0} on node: {1} with clientInfo: {2}",
                                new Object[]{id, node, clientInfo, });
                    }
                    StashInfo lockedInfo = new StashInfo(StashInfo.StashType.EXTERNAL, lockedBy, null, null);
                    resource.doLock(lockedInfo);
                    sendOk(response);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Failed to save the changes to disk, but the resource state has changed.", response);
                }
            }
        };
        doSomething(node, id, something, response);
    }

    /**
     * Signal from an external lock handler that an {@link ExternalResource} has been reserved.
     *
     * @param node     the node where the resource is located.
     * @param id       the id of the resource.
     * @param reservedBy a String describing what has reserved the resource.
     * @param clientInfo the information about the client that called this.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see CliResponse
     */
    @SuppressWarnings("unused")
    public void doReserveResource(
            @QueryParameter(value = "node", required = true) final String node,
            @QueryParameter(value = "id", required = true) final String id,
            @QueryParameter(value = "reservedBy", required = true) final String reservedBy,
            @QueryParameter(value = "clientInfo", required = true) final String clientInfo,
        StaplerResponse response) throws IOException {
        Something something = new Something() {
            @Override
            public void doIt(ExternalResource resource, StaplerResponse response) throws IOException {
                try {
                    if (ErCliUtils.isRequestCircular(clientInfo)) {
                        logger.log(Level.FINE, "Request was circular for: {0} ", clientInfo);
                        return;
                    }

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Reserving resource: {0} on node: {1} with clientInfo: {2}",
                                new Object[]{id, node, clientInfo, });
                    }
                    StashInfo reservedInfo = new StashInfo(StashInfo.StashType.EXTERNAL, reservedBy, null, null);
                    resource.doReserve(reservedInfo);
                    sendOk(response);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Failed to save the changes to disk, but the resource state has changed.", response);
                }
            }
        };
        doSomething(node, id, something, response);
    }

    /**
     * Signal from an external lock handler that an {@link ExternalResource} has been released.
     *
     * @param node     the node where the resource is located.
     * @param id       the id of the resource.
     * @param clientInfo the information about the client that called this.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see CliResponse
     */
    @SuppressWarnings("unused")
    public void doReleaseResource(
            @QueryParameter(value = "node", required = true) final String node,
            @QueryParameter(value = "id", required = true) final String id,
            @QueryParameter(value = "clientInfo", required = true) final String clientInfo,
        StaplerResponse response) throws IOException {
        Something something = new Something() {
            @Override
            public void doIt(ExternalResource resource, StaplerResponse response) throws IOException {
                try {
                    if (ErCliUtils.isRequestCircular(clientInfo)) {
                        logger.log(Level.FINE, "Request was circular for: {0}", clientInfo);
                        return;
                    }
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Releasing resource: {0} on node: {1} with clientInfo: {2}",
                                new Object[]{id, node, clientInfo, });
                    }
                    resource.doRelease();
                    sendOk(response);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Failed to save the changes to disk, but the resource state has changed.", response);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not release resource! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Could not release resource.", response);
                }
            }
        };
        doSomething(node, id, something, response);
    }

    /**
     * Does something with an external resource.
     *
     * @param node the node where the resource is located.
     * @param id the id of the resource.
     * @param something the actual Operation to perform.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see #doEnable(String, String, org.kohsuke.stapler.StaplerResponse)
     * @see #doDisable(String, String, org.kohsuke.stapler.StaplerResponse)
     * @see ExternalResource#doEnable(boolean)
     * @see Something
     */
    private void doSomething(final String node, final String id, Something something, StaplerResponse response)
            throws IOException {
        Node theNode = Hudson.getInstance().getNode(node);
        if (theNode != null) {
            ExternalResource resource = AvailabilityFilter.getInstance().getExternalResourceById(theNode, id);
            if (resource != null) {
                something.doIt(resource, response);
            } else {
                CliResponse.sendError(CliUtils.Status.ERR_NO_METADATA,
                        "No resource with id " + id + " exists on this node.", response);
            }
        } else {
            CliResponse.sendError(CliUtils.Status.ERR_NO_ITEM,
                    "No node with name " + node + " exists on this Jenkins server.", response);
        }
    }

    /**
     * Interface for performing an action on an {@link ExternalResource}.
     * It is fed to the method
     * {@link ExternalResourceHttpCommands#
     *             doSomething(String, String, ExternalResourceHttpCommands.Something, StaplerResponse)}
     * so that all commands on a particular resource can have a uniform behaviour.
     */
    private interface Something {
        /**
         * Performs the intended action on the resource.
         *
         * @param resource the resource to perform the action on.
         * @param response the response handle to send an eventual ok or specific error on.
         * @throws IOException if any response failed to be sent.
         */
        void doIt(ExternalResource resource, StaplerResponse response) throws IOException;
    }

}
