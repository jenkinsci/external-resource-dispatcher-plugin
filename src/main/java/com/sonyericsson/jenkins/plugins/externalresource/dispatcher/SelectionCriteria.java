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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher;

import java.util.LinkedList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.AbstractDeviceSelection;


/**
 * Holder for the user specified criteria about what resource the build wants to use.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@ExportedBean
public class SelectionCriteria extends JobProperty<AbstractProject<?, ?>> {

    private List<AbstractDeviceSelection> deviceSelectionList;

    /**
     * Standard DataBound Constructor.
     *
     * @param deviceSelectionList the selection list
     */
    @DataBoundConstructor
    public SelectionCriteria(List<AbstractDeviceSelection> deviceSelectionList) {
        this.deviceSelectionList = deviceSelectionList;
    }

    /**
     * The list of device selection, if null, then create a new list.
     *
     * @return all the device selections
     */
    public synchronized List<AbstractDeviceSelection> getDeviceSelectionList() {
        if (deviceSelectionList == null) {
            deviceSelectionList = new LinkedList<AbstractDeviceSelection>();
        }
        return deviceSelectionList;
    }

    /**
     * Get matching resource from available resources.
     *
     * @param availableResourceList
     *            available resources list.
     * @return the matching resource list if exists.
     */
    public List<ExternalResource> getMatchingResources(List<ExternalResource> availableResourceList) {
        List<ExternalResource> matchingResourceList = new LinkedList<ExternalResource>();
        boolean foundFlag = true;
        for (ExternalResource er : availableResourceList) {
            foundFlag = true;
            for (AbstractDeviceSelection deviceSelection : deviceSelectionList) {
                if (!deviceSelection.equalToExternalResourceValue(er)) {
                    foundFlag = false;
                    break;
                }
            }
            if (foundFlag) {
                matchingResourceList.add(er);
            }
        }
        return matchingResourceList;
     }

    /**
     * Descriptor for {@link SelectionCriteria}.
     */
    @Extension
    public static class SelectionCriteriaDescriptor extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.SelectionCriteria_DisplayName();
        }

        /**
         * All registered device selection descriptors that applies to jobs. To be used by a hetero-list.
         *
         * Get descriptor list.
         * @return the descriptor list.
         */
        public List<AbstractDeviceSelection.AbstractDeviceSelectionDescriptor> getDeviceSelectionDescriptors() {
            return Hudson.getInstance()
                    .getExtensionList(AbstractDeviceSelection.AbstractDeviceSelectionDescriptor.class);

        }
    }
}
