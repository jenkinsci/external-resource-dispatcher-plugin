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

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseNoAvailableResources;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseNoMatchingResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseNothingReserved;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager;
import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

import java.util.List;

/**
 * The main veto engine.
 * If a node has an available and matching resource, the resource will be reserved and OKd to be scheduled on that node.
 * Otherwise the build will be blocked from that Node.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension(ordinal = Constants.QTD_ORDINAL)
public class ExternalResourceQueueTaskDispatcher extends QueueTaskDispatcher {

    @Override
    public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {

        SelectionCriteria selectionCriteria = getSelectionCriteria(item.task);
        if (selectionCriteria == null
                || selectionCriteria.getDeviceSelectionList() == null
                || selectionCriteria.getDeviceSelectionList().isEmpty()) {
            //Either it is not a buildable item that we are interested in, or it is a project that
            // doesn't have a configured criteria. So we say ok.
            return null;
        }
        //TODO instead of checking for an empty selection list or not, make a better UI with an optional block.

        AvailabilityFilter availabilityFilter = AvailabilityFilter.getInstance();

        //Find all resources
        List<ExternalResource> resources = availabilityFilter.getExternalResourcesList(node);
        if (resources == null || resources.isEmpty()) {
            //No resources configured, block the build on this node.
            return new BecauseNoAvailableResources(node);
        }
        //Filter out what is available
        resources = availabilityFilter.filterEnabledAndAvailable(resources);
        if (resources == null || resources.isEmpty()) {
            //No available resources, block the build on this node.
            return new BecauseNoAvailableResources(node);
        }

        resources = selectionCriteria.getMatchingResources(resources);

        if (resources == null || resources.isEmpty()) {
            //No matching resources, block the build on this node.
            return new BecauseNoMatchingResource(node);
        }

        //Reserve something
        ExternalResource reservedResource = null;
        ExternalResourceManager manager = PluginImpl.getInstance().getManager();

        for (ExternalResource resource : resources) {
            StashResult result = manager.reserve(node, resource, PluginImpl.getInstance().getReserveTime());
            if (result != null && result.isOk()) {
                reservedResource = resource;
                StashInfo info = new StashInfo(result, getUrl(item.task));
                reservedResource.setReserved(info);
                break;
            }
        }

        if (reservedResource == null) {
            //None of the matching resources could be reserved, block the build
            //TODO notify admin?
            return new BecauseNothingReserved(node);
        }

        //TODO set metadata on build, probably on the item.future object, needs investigation.

        //Everything is fine, now continue.
        return null;
    }

    /**
     * Gets the internal URL to the build to be, or just the project if we can't find one.
     *
     * @param task the task
     * @return the url to the "task"
     */
    private String getUrl(Queue.Task task) {
        AbstractProject<?, ?> p = getProject(task);
        if (p != null) {
            //TODO find the build and return that URL if possible.
            return p.getAbsoluteUrl();
        }
        return "";
    }

    /**
     * Gets the project that this task represents.
     *
     * @param task the task
     * @return the project.
     */
    private AbstractProject getProject(Queue.Task task) {
        if (task instanceof AbstractProject) {
            AbstractProject<?, ?> p = (AbstractProject<?, ?>)task;
            if (task instanceof MatrixConfiguration) {
                p = (AbstractProject<?, ?>)((MatrixConfiguration)task).getParent();
            }
            return p;
        }
        return null;
    }

    /**
     * Gets the selection criteria from the task if there is any.
     *
     * @param task the task.
     * @return the selection criteria or null if there is none configured.
     */
    private SelectionCriteria getSelectionCriteria(Queue.Task task) {
        AbstractProject<?, ?> p = getProject(task);
        if (p != null) {
            return p.getProperty(SelectionCriteria.class);
        }
        return null;
    }
}
