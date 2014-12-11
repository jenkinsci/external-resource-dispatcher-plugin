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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection;

import hudson.model.Describable;
import hudson.model.Descriptor;

import java.io.Serializable;

import hudson.model.Queue;
import org.kohsuke.stapler.export.ExportedBean;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;



/**
 * Abstract ResourceSelection.
 *
 * @author Ren Wei &lt;wei2.ren@sonyericsson.com&gt;
 *
 */
@ExportedBean
public abstract class AbstractResourceSelection implements Serializable, Describable<AbstractResourceSelection> {
    /**
     * Descriptor for {@link AbstractResourceSelection}.
     */
    public abstract static class AbstractResourceSelectionDescriptor extends Descriptor<AbstractResourceSelection> {
    }
    /**
     * Resource selection input compare With ExternalResource leaf value.
     *
     * @param externalResource the External Resource
     * @return true if resource selection equals to ExternalResource leaf value
     */
    public abstract boolean equalToExternalResourceValue(ExternalResource externalResource);
    /**
     * Resource selection input compare With ExternalResource leaf value.
     * Takes a {@link hudson.model.Queue.Item} that can be queried for e.g. build parameter values.
     *
     * @param externalResource the External Resource
     * @param qi Parent queue item
     * @return true if resource selection equals to ExternalResource leaf value
     */
    public abstract boolean equalToExternalResourceValue(ExternalResource externalResource, Queue.Item qi);

}
