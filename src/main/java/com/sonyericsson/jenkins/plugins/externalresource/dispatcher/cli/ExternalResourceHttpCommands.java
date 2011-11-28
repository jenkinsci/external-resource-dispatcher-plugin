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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.cli;

import com.sonyericsson.hudson.plugins.metadata.cli.CliResponse;
import com.sonyericsson.hudson.plugins.metadata.cli.CliUtils;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
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

import static com.sonyericsson.hudson.plugins.metadata.cli.CliResponse.Type;
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
        doIt(node, id, true, response);
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
        doIt(node, id, false, response);
    }

    /**
     * Does the enable/disable.
     * @param node the node where the resource is located.
     * @param id the id of the resource.
     * @param enable true to enable, false to disable.
     * @param response the response handle to write to.
     * @throws IOException if so.
     * @see #doEnable(String, String, org.kohsuke.stapler.StaplerResponse)
     * @see #doDisable(String, String, org.kohsuke.stapler.StaplerResponse)
     * @see ExternalResource#doEnable(boolean)
     */
    private void doIt(final String node, final String id, boolean enable, StaplerResponse response)
            throws IOException {
        Node theNode = Hudson.getInstance().getNode(node);
        if (node != null) {
            ExternalResource resource = AvailabilityFilter.getInstance().getExternalResourceById(theNode, id);
            if (resource != null) {
                try {
                    resource.doEnable(enable);
                    sendOk(response);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
                    sendResponse(Type.warning, 0, "Warning",
                            "Failed to save the changes to disk, but the resource state has changed.", response);
                }
            } else {
                CliResponse.sendError(CliUtils.Status.ERR_NO_METADATA,
                        "No resource with id " + id + " exists on this node.", response);
            }
        } else {
            CliResponse.sendError(CliUtils.Status.ERR_NO_ITEM,
                    "No node with name " + node + " exists on this Jenkins server.", response);
        }
    }
}
