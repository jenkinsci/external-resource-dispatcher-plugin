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
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.AbstractResourceSelection;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AdminNotifier;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.ExternalResourceManager;
import hudson.model.*;


import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.Extension;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;

import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.BUILD_LOCKED_RESOURCE_NAME;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.getBuildLockedResourceParentPath;


/**
 * Holder for the user specified criteria about what resource the build wants to use.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@ExportedBean
public class SelectionCriteria extends JobProperty<AbstractProject<?, ?>> {

    private static final Logger logger = Logger.getLogger(SelectionCriteria.class.getName());
    private boolean selectionEnabled;
    private List<AbstractResourceSelection> resourceSelectionList;

    /**
     * Standard DataBound Constructor.
     *
     * @param selectionEnabled if true, selection is checked
     * @param resourceSelectionList the selection list
     */
    @DataBoundConstructor
    public SelectionCriteria(boolean selectionEnabled, List<AbstractResourceSelection> resourceSelectionList) {
        this.selectionEnabled = selectionEnabled;
        this.resourceSelectionList = resourceSelectionList;
    }

    /**
     * Standard Constructor.
     *
     * @param resourceSelectionList the selection list
     */
    public SelectionCriteria(List<AbstractResourceSelection> resourceSelectionList) {
        this.resourceSelectionList = resourceSelectionList;
    }

    /**
     * The list of resource selection, if null, then create a new list.
     *
     * @return all the resource selections
     */
    public synchronized List<AbstractResourceSelection> getResourceSelectionList() {
        if (resourceSelectionList == null) {
            resourceSelectionList = new LinkedList<AbstractResourceSelection>();
        }
        return resourceSelectionList;
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
     * Takes an additional {@link hudson.model.Queue.Item} that can be queried for e.g. build parameter values.
     *
     * @param availableResourceList
     *            available resources list.
     * @param qi parent queue item.
     * @return the matching resource list if exists.
     */
    public List<ExternalResource> getMatchingResources(List<ExternalResource> availableResourceList, Queue.Item qi) {
        List<ExternalResource> matchingResourceList = new LinkedList<ExternalResource>();
        boolean foundFlag = true;
        for (ExternalResource er : availableResourceList) {
            foundFlag = true;
            for (AbstractResourceSelection resourceSelection : resourceSelectionList) {
                if (!resourceSelection.equalToExternalResourceValue(er, qi)) {
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
            for (AbstractResourceSelection resourceSelection : resourceSelectionList) {
                if (!resourceSelection.equalToExternalResourceValue(er)) {
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
            StashResult result = resourceManager.reserve(node, reserved, PluginImpl.getInstance().getReserveTime(),
                    build.getDisplayName());
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
        StashResult lockResult = resourceManager.lock(node, reserved, reservedInfo.getKey(),
                build.getUrl());
        if (lockResult == null || !lockResult.isOk()) {
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR, AdminNotifier.OperationType.LOCK,
                    node, reserved, "Could not lock resource, aborting the build: " + buildName);
            logger.log(Level.SEVERE, "Could not lock resource: [{0}], aborting the build: [{1}].",
                    new String[]{reserved.getId(), buildName});
            listener.getLogger().println("Could not lock resource: " + reserved.getId() + ", aborting the build.");
            return false;
        }
        //update the node and build information.
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
        TreeNodeMetadataValue lockedTree = TreeStructureUtil.createPath(locked, getBuildLockedResourceParentPath());
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
         * All registered resource selection descriptors that applies to jobs. To be used by a hetero-list.
         *
         * Get descriptor list.
         * @return the descriptor list.
         */
        public List<AbstractResourceSelection.AbstractResourceSelectionDescriptor> getResourceSelectionDescriptors() {
            return Hudson.getInstance()
                    .getExtensionList(AbstractResourceSelection.AbstractResourceSelectionDescriptor.class);

        }
    }
}
