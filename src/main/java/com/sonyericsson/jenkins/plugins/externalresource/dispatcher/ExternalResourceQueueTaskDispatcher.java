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

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ReservedExternalResourceAction;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseNoAvailableResources;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseNoMatchingResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseNothingReserved;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.veto.BecauseAlreadyReserved;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AdminNotifier;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.ExternalResourceManager;
import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main veto engine. If a node has an available and matching resource, the resource will be reserved and OKd to be
 * scheduled on that node. Otherwise the build will be blocked from that Node.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension(ordinal = Constants.QTD_ORDINAL)
public class ExternalResourceQueueTaskDispatcher extends QueueTaskDispatcher {

    private static final Logger logger = Logger.getLogger(ExternalResourceQueueTaskDispatcher.class.getName());

    @Override
    public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {
        logger.entering("ExternalResourceQueueTaskDispatcher", "canTake", new Object[]{node, item});
        // check whether there is already something reserved for use. skip the following step if so.
        // the cantake() method will be called several times, depending on how many available executors left.
        ReservedExternalResourceAction storage = getReservedExternalResourceAction(item);
        if (!storage.isEmpty()) { // if already something there.
            // return a blockage cause to avoid the executor joining to candidates list once we already have one.
            logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "BecauseAlreadyReserved");
            return new BecauseAlreadyReserved();
        }

        SelectionCriteria selectionCriteria = getSelectionCriteria(item.task);
        if (selectionCriteria == null
                || !selectionCriteria.getSelectionEnabled()
                || selectionCriteria.getResourceSelectionList() == null
                || selectionCriteria.getResourceSelectionList().isEmpty()) {
            //Either it is not a buildable item that we are interested in, or it is a project that
            // doesn't have a configured criteria. So we say ok.
            logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "OK - not buildable or no selection");
            return null;
        }

        AvailabilityFilter availabilityFilter = AvailabilityFilter.getInstance();

        //Find all resources
        List<ExternalResource> resources = availabilityFilter.getExternalResourcesList(node);
        if (resources == null || resources.isEmpty()) {
            //No resources configured, block the build on this node.
            logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "BecauseNoAvailableResources-1");
            return new BecauseNoAvailableResources(node);
        }
        //Filter out what is available
        resources = availabilityFilter.filterEnabledAndAvailable(resources);
        if (resources == null || resources.isEmpty()) {
            //No available resources, block the build on this node.
            logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "BecauseNoAvailableResources-2");
            return new BecauseNoAvailableResources(node);
        }

        resources = selectionCriteria.getMatchingResources(resources);

        if (resources == null || resources.isEmpty()) {
            //No matching resources, block the build on this node.
            logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "BecauseNoMatchingResource");
            return new BecauseNoMatchingResource(node);
        }

        //Reserve something
        ExternalResource reservedResource = null;
        ExternalResourceManager manager = PluginImpl.getInstance().getManager();

        for (ExternalResource resource : resources) {
            StashResult result = manager.reserve(node, resource, PluginImpl.getInstance().getReserveTime(),
                    item.task.getUrl());
            logger.log(Level.FINEST, "Reserve result for [{0}]: Status {1} code {2} message {3}",
                    new Object[]{resource.getFullName(), result.getStatus().name(),
                            result.getErrorCode(), result.getMessage(), });
            if (result != null && result.isOk()) {
                reservedResource = resource;
                logger.finest("reservation ok");
                break;
            } else {
                logger.finest("Not reserved");
            }
        }

        if (reservedResource == null) {
            //None of the matching resources could be reserved, block the build
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.WARNING, AdminNotifier.OperationType.RESERVE,
                    node, reservedResource, "Found one or more matching external resources but could not reserve any "
                            + "of them.");
            logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "BecauseNothingReserved");
            return new BecauseNothingReserved(node);
        }

        //Cannot create a metadata action since it requires a build. Temporarily storing it in a separate action.
        storage.push(reservedResource);


        //Everything is fine, now continue.
        logger.exiting("ExternalResourceQueueTaskDispatcher", "canTake", "OK");
        return null;
    }

    /**
     * Finds the action or creates and adds it to the item if it doesn't exsist.
     *
     * @param item the build to be.
     * @return the storage.
     */
    private ReservedExternalResourceAction getReservedExternalResourceAction(Queue.BuildableItem item) {
        List<ReservedExternalResourceAction> actions = item.getActions(ReservedExternalResourceAction.class);
        // maintain the actions for run out leases
        Iterator<ReservedExternalResourceAction> iterator = actions.iterator();
        List<ReservedExternalResourceAction> toRemove = new ArrayList<ReservedExternalResourceAction>();
        while (iterator.hasNext()) {
            ReservedExternalResourceAction act = iterator.next();
            act.maintain();
            if (act.isEmpty()) {
                toRemove.add(act);
            }
        }
        if (!toRemove.isEmpty()) {
            item.getActions().removeAll(toRemove);
            actions.removeAll(toRemove);
        }

        if (actions != null && actions.size() > 0) {
            return actions.get(0);
        } else {
            ReservedExternalResourceAction storage = new ReservedExternalResourceAction();
            item.addAction(storage);
            return storage;
        }
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
