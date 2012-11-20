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


import hudson.Extension;

import hudson.model.Descriptor;
import hudson.model.Hudson;

import org.kohsuke.stapler.DataBoundConstructor;

import com.sonyericsson.hudson.plugins.metadata.model.Metadata;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.
        STRING_RESOURCE_SELECTION_SEPARATOR_WITH_ESCAPE;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A Selection Criteria value of the type String.
 *
 * @author Ren Wei &lt;wei2.ren@sonyericsson.com&gt;
 */
@XStreamAlias("resourceSelection-String")
public class StringResourceSelection extends AbstractResourceSelection {

    private String name;
    private String value;

    /**
     * Standard Constructor.
     *
     * @param name the name.
     * @param value the value.
     */
    @DataBoundConstructor
    public StringResourceSelection(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public Descriptor<AbstractResourceSelection> getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(StringResourceSelectionDescriptor.class);
    }

    /**
     * Get the StringResourceSelection name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
    * Get the StringResourceSelection value.
    *
    * @return value
    */
    public String getValue() {
        return value;
    }

    /**
    * The descriptor of {@link StringResourceSelection}.
    */
    @Extension
    public static class StringResourceSelectionDescriptor extends AbstractResourceSelectionDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.StringResourceSelection_DisplayName();
        }

    }
    @Override
    public boolean equalToExternalResourceValue(ExternalResource externalResource) {
        String[] path = name.split(STRING_RESOURCE_SELECTION_SEPARATOR_WITH_ESCAPE);
        Metadata externalResourceValue = TreeStructureUtil.getLeaf(externalResource, path);
        if (externalResourceValue != null) {
            Object tmpValue = externalResourceValue.getValue();
            if (tmpValue != null && value.equals(tmpValue.toString())) {
                return true;
            }
        }
        return false;
    }
}
