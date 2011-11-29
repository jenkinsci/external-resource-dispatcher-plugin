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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto;

import hudson.model.Node;
import hudson.model.queue.CauseOfBlockage;

/**
 * Base CauseOfBlockage for all
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource}
 * related vetoes.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public abstract class BecauseOfExternalResource extends CauseOfBlockage {
    /**
     * The node that this is all about.
     */
    protected Node node;

    /**
     * Standard constructor.
     *
     * @param node who it is all about.
     */
    protected BecauseOfExternalResource(Node node) {
        this.node = node;
    }

    /**
     * The name of {@link #node}.
     *
     * @return the displayName or [UNKNOWN] if node is null.
     */
    protected String getNodeName() {
        String name = "[UNKNOWN]";
        if (node != null) {
            name = node.getDisplayName();
        }
        return name;
    }
}
