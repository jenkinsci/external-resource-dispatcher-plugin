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

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter;
import hudson.model.Hudson;
import hudson.model.Node;
import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.kohsuke.args4j.CmdLineException;

/**
 * Common utility functions for Cli Commands.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public final class ErCliUtils {

    /**
     * Finds the {@link ExternalResource} on the node with the given id.
     *
     * @param nodeName the name of the node to look in.
     * @param id       the id of the external resource.
     * @return the resource if found.
     *
     * @throws CmdLineException if no node or resource could be found.
     */
    public static ExternalResource findExternalResource(String nodeName, String id) throws CmdLineException {
        Node node = Hudson.getInstance().getNode(nodeName);
        if (node != null) {
            ExternalResource resource = AvailabilityFilter.getInstance().getExternalResourceById(node, id);
            if (resource != null) {
                return resource;
            } else {
                throw new CmdLineException(null, "No resource with id " + id + " exists on this node.");
            }
        } else {
            throw new CmdLineException(null, "No node with name " + nodeName + " exists on this Jenkins server.");
        }
    }

    /**
     * Checks if a request came originally from Jenkins itself.
     * If so, the request should be ignored.
     * @param clientInfo the JSON String containing information on the client that sent the request.
     * @return true if the client is Jenkins, false if not.
     */
    public static boolean isRequestCircular(String clientInfo) {
        JSON json = JSONSerializer.toJSON(clientInfo);
        JSONObject obj = null;
        if (json instanceof JSONObject) {
            obj = (JSONObject)json;
        }
        if (obj == null) {
            return false;
        }
        String clientId = "";
        try {
            clientId = obj.getString("id");
        } catch (JSONException e) {
            return false;
        }

        String rootUrl = Hudson.getInstance().getRootUrl();
        if (clientId.equals(rootUrl)) {
            //Request came from Jenkins originally, do nothing.
            return true;
        }
        return false;
    }

    /**
     * Utility Constructor.
     */
    private ErCliUtils() {

    }
}
