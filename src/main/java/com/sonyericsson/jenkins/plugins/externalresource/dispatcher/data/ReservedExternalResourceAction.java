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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data;

import hudson.model.Action;

import java.util.Stack;

/**
 * Information carrier from the
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.ExternalResourceQueueTaskDispatcher}
 * to the runlistener who shall lock the resource later on. Since the QTD doesn't have a build to work with that is
 * required when creating a {@link com.sonyericsson.hudson.plugins.metadata.model.MetadataBuildAction}. This class
 * serves as a temporary storage. The storage consists of a stack of all the reserved resources for the future build.
 * The intent is to have a history of the resources where the first one in the stack is the latest one reserved. The
 * other resources could then be released one by one if they haven't been already.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class ReservedExternalResourceAction implements Action {

    private Stack<ExternalResource> stack = new Stack<ExternalResource>();

    @Override
    public String getIconFileName() {
        //Do not show the action, it is just a data container.
        return null;
    }

    @Override
    public String getDisplayName() {
        //Do not show the action, it is just a data container.
        return null;
    }

    @Override
    public String getUrlName() {
        //Do not show the action, it is just a data container.
        return null;
    }

    /**
     * Pops the internal stack.
     *
     * @return the top resource.
     *
     * @see java.util.Stack#pop()
     */
    public synchronized ExternalResource pop() {
        return stack.pop();
    }

    /**
     * pushes a resource onto the internal stack.
     *
     * @param resource the new top resource.
     * @return the new top resource.
     *
     * @see java.util.Stack#push(Object)
     */
    public synchronized ExternalResource push(ExternalResource resource) {
        return stack.push(resource);
    }

    /**
     * See what is on top of the internal stack without modifying the it..
     *
     * @return the top resource.
     *
     * @see java.util.Stack#push(Object)
     */
    public synchronized ExternalResource peek() {
        return stack.peek();
    }
}
