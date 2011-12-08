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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sonyericsson.hudson.plugins.metadata.model.MetadataBuildAction;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ReservedExternalResourceAction;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AdminNotifier;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.AbstractDeviceSelection;

import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.BUILD_LOCKED_RESOURCE_PARENT_PATH;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.BUILD_LOCKED_RESOURCE_NAME;


/**
 * Holder for the user specified criteria about what resource the build wants to use.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@ExportedBean
public class SelectionCriteria extends JobProperty<AbstractProject<?, ?>> {

    private static final Logger logger = Logger.getLogger(SelectionCriteria.class.getName());
    private boolean selectionEnabled;
    private List<AbstractDeviceSelection> deviceSelectionList;

    /**
     * Standard DataBound Constructor.
     *
     * @param selectionEnabled if true, selection is checked
     * @param deviceSelectionList the selection list
     */
    @DataBoundConstructor
    public SelectionCriteria(boolean selectionEnabled, List<AbstractDeviceSelection> deviceSelectionList) {
        this.selectionEnabled = selectionEnabled;
        this.deviceSelectionList = deviceSelectionList;
    }

    /**
     * Standard Constructor.
     *
     * @param deviceSelectionList the selection list
     */
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
     * SelectionEnabled value.
     *
     * @return true if selection is enabled
     */
    public boolean getSelectionEnabled() {
        return selectionEnabled;
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

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build,
                        BuildListener listener) {
        if (!getSelectionEnabled()) {
            logger.log(Level.FINE, "Selection not enabled, continuing");
            return true;
        }
        String buildName = build.getFullDisplayName();
        Node node = build.getBuiltOn();
        ReservedExternalResourceAction action = build.getAction(ReservedExternalResourceAction.class);
        if (action == null) {
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR, AdminNotifier.OperationType.RESERVE,
                    node, null, "No phone chosen even though we have selection criteria, aborting build: "
                            + buildName);
            logger.log(Level.SEVERE,
                    "No phone chosen even though we have selection criteria, aborting build: [{0}].", buildName);
            listener.getLogger().println(
                    "No phone chosen even though we have selection criteria, aborting build.");
            return false;
        }
        ExternalResource reserved = action.pop();
        StashInfo reservedInfo = reserved.getReserved();
        ExternalResourceManager resourceManager = PluginImpl.getInstance().getManager();

        //If the phone is not reserved anymore, try to reserve it again.
        //If it cannot be reserved, fail the build.
        if (reservedInfo == null) {
            StashResult result = resourceManager.reserve(node, reserved, PluginImpl.getInstance().getReserveTime());
            if (result == null || !result.isOk()) {
                AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR,
                        AdminNotifier.OperationType.RESERVE, node, reserved,
                        "The external resource has been taken by someone else, aborting build: " + buildName);
                logger.log(Level.SEVERE, "External resource: [{0}] has been taken by someone else, aborting build",
                        reserved.getId());
                listener.getLogger().println("External resource: " + reserved.getId()
                        + " has been taken by someone else, aborting build");
                return false;
            } else {
                reservedInfo = new StashInfo(result, build.getUrl());
            }
        }
        //we have a reserved phone, now lock it.
        StashResult lockResult = resourceManager.lock(node, reserved, reservedInfo.getKey());
        if (lockResult == null || !lockResult.isOk()) {
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR, AdminNotifier.OperationType.LOCK,
                    node, reserved, "Could not lock device, aborting the build: " + buildName);
            logger.log(Level.SEVERE, "Could not lock device: [{0}], aborting the build: [{1}].",
                    new String[]{reserved.getId(), buildName});
            listener.getLogger().println("Could not lock device: " + reserved.getId() + ", aborting the build.");
            return false;
        }
        //update the node and build information.
        StashInfo lockInfo = new StashInfo(lockResult, build.getUrl());
        // change the reserved info and set lock info.
        reserved.setLocked(lockInfo);
        reserved.setReserved(null);
        ExternalResource locked;
        try {
            locked = reserved.clone();
        } catch (CloneNotSupportedException e) {
            //should not happen since ExternalResource and its ancestors are cloneable.
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR, AdminNotifier.OperationType.LOCK,
                    node, reserved, "Could not clone the External resource, aborting the build: " + buildName);
            logger.log(Level.SEVERE,
                    "Could not clone the External resource: [{0}], aborting the build: [{1}].",
                    new String[]{reserved.getId(), buildName});
            listener.getLogger().println(
                    "Could not clone the External resource: " + reserved.getId() + ", aborting the build.");
            return false;
        }
        MetadataBuildAction metadataBuildAction = build.getAction(MetadataBuildAction.class);
        if (metadataBuildAction == null) {
            metadataBuildAction = new MetadataBuildAction(build);
            build.addAction(metadataBuildAction);
        }
        locked.setName(BUILD_LOCKED_RESOURCE_NAME);
        TreeNodeMetadataValue lockedTree = TreeStructureUtil.createPath(locked, BUILD_LOCKED_RESOURCE_PARENT_PATH);
        metadataBuildAction.addChild(lockedTree);
        locked.setExposeToEnvironment(true);
        //The resource has been locked and we can continue with the build.
        return true;
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
