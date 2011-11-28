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

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import hudson.Extension;
import hudson.cli.CLICommand;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enables an {@link ExternalResource} on a Node.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 * @see com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource#doEnable(boolean)
 */
@Extension
public class EnableExternalResourceCommand extends CLICommand {

    private static final Logger logger = Logger.getLogger(EnableExternalResourceCommand.class.getName());

    //CS IGNORE VisibilityModifier FOR NEXT 11 LINES. REASON: Standard Jenkins Args4J design pattern.

    /**
     * The name of the node.
     */
    @Option(required = true, name = "-node", usage = "The name of the node")
    public String nodeName;
    /**
     * The id of the resource.
     */
    @Option(required = true, name = "-id", usage = "The id of the external resource")
    public String id;

    @Override
    public String getShortDescription() {
        return Messages.EnableExternalResourceCliCommand_Description();
    }

    @Override
    protected int run() throws Exception {
        ExternalResource er = ErCliUtils.findExternalResource(nodeName, id);
        try {
            er.doEnable(true);
            stdout.println("OK");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Probably failed to save the node config to disk! ", e);
            stderr.println("Warning: The resource is enabled, but the changes failed to be saved to disk.");
        }
        return 0;
    }
}
