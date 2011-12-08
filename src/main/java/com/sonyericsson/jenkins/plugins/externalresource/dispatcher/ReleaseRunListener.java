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

import com.sonyericsson.hudson.plugins.metadata.model.MetadataBuildAction;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AdminNotifier;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RunListener in charge of releasing a locked
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource}
 * if the build has any.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@Extension
public class ReleaseRunListener extends RunListener<AbstractBuild> {

    private static final Logger logger = Logger.getLogger(ReleaseRunListener.class.getName());

    @Override
    public void onCompleted(AbstractBuild build, TaskListener listener) {
        logger.entering("ReleaseRunListener", "onCompleted", build);
        MetadataBuildAction metadata = build.getAction(MetadataBuildAction.class);
        if (metadata != null) {
            MetadataValue value = TreeStructureUtil.getPath(metadata, Constants.BUILD_LOCKED_RESOURCE_PATH);
            if (value != null && value instanceof ExternalResource) {
                ExternalResource buildResource = (ExternalResource)value;
                PrintStream buildLogger = listener.getLogger();
                release(build, buildResource, buildLogger);
            } else {
                logger.log(Level.FINE, "No locked resource found: {0}", value);
            }
        } else {
            logger.log(Level.FINE, "No metadata attached to build {0}.", build);
        }
        logger.exiting("ReleaseRunListener", "onCompleted");
    }

    /**
     * Performs the release.
     *
     * @param build         the build.
     * @param buildResource the resource instance that is attached to the build.
     * @param buildLogger   something to inform the users with.
     */
    private void release(AbstractBuild build, ExternalResource buildResource, PrintStream buildLogger) {
        buildLogger.println("Releasing previously locked resource: " + buildResource.getId() + "...");
        logger.log(Level.FINE, "Releasing previously locked resource: [{0}] for build [{1}]",
                new String[]{buildResource.getId(), build.getFullDisplayName()});
        ExternalResource nodeResource = AvailabilityFilter.getInstance()
                .getExternalResourceById(build.getBuiltOn(), buildResource.getId());
        if (nodeResource != null) {
            StashInfo lockInfo = nodeResource.getLocked();
            if (lockInfo != null) {
                StashResult result = PluginImpl.getInstance().getManager().release(build.getBuiltOn(),
                        nodeResource, lockInfo.getKey());
                if (result != null && result.isOk()) {
                    //Success!
                    logReleaseSuccess(build, buildResource, buildLogger);
                    nodeResource.setLocked(null);
                } else {
                    logReleaseFailure(build, buildLogger, nodeResource, result);
                }
            } else {
                logWarningPreReleased(build, buildLogger, nodeResource);
            }
        } else {
            logNoLongerAttached(build, buildResource, buildLogger);
        }
        buildResource.setLocked(null);
    }

    /**
     * Inform the user and admins that the resource that should be released is no longer attached to the node.
     *
     * @param build         the build
     * @param buildResource the resource instance attached to the build.
     * @param buildLogger   to inform the user with.
     */
    private void logNoLongerAttached(AbstractBuild build, ExternalResource buildResource, PrintStream buildLogger) {
        AdminNotifier.getInstance().notify(AdminNotifier.MessageType.WARNING, AdminNotifier.OperationType.RELEASE,
                        build.getBuiltOn(), buildResource,
                "The external resource is no longer attached to the node. Skipping release");
        buildLogger.println("The external resource is no longer attached to the node. Skipping release");
        logger.log(Level.WARNING, "The external resource [{0}] is no longer attached to the node [{1}]."
                + " Skipping release.",
                new String[]{buildResource.getId(), build.getBuiltOn().getNodeName()});
    }

    /**
     * Inform the user and admins that the resource has already been unlocked by some other means.
     *
     * @param build        the build
     * @param buildLogger  the resource attached to the build.
     * @param nodeResource the resource instance attached to the node.
     */
    private void logWarningPreReleased(AbstractBuild build, PrintStream buildLogger, ExternalResource nodeResource) {
        AdminNotifier.getInstance().notify(AdminNotifier.MessageType.WARNING, AdminNotifier.OperationType.RELEASE,
                        build.getBuiltOn(), nodeResource, "The resource has already been unlocked by some other means");
        buildLogger.println("WARNING The resource has already been unlocked by some other"
                + " means. The Build might have suffered from it.");
        logger.log(Level.WARNING, "The resource [{0}] on node [{1}] has already been unlocked"
                + " by some other means",
                new String[]{nodeResource.getId(), build.getBuiltOn().getNodeName()});
    }

    /**
     * Inform the user that the resource was successfully released.
     *
     * @param build         the build
     * @param buildResource the resource attached to the build.
     * @param buildLogger   to inform the user with.
     */
    private void logReleaseSuccess(AbstractBuild build, ExternalResource buildResource, PrintStream buildLogger) {
        buildLogger.println("Resource " + buildResource.getId() + " successfully released.");
        logger.log(Level.FINE, "Resource [{0}] successfully released from build [{1}]",
                new String[]{buildResource.getId(), build.getFullDisplayName()});
    }

    /**
     * Inform the user and admins that the manager failed to release the resource.
     *
     * @param build        the build
     * @param buildLogger  to inform the user with.
     * @param nodeResource the resource instance attached to the node.
     * @param result       the result returned by the manager.
     * @see com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager
     *      #release(hudson.model.Node, ExternalResource, String)
     */
    private void logReleaseFailure(AbstractBuild build, PrintStream buildLogger,
                                   ExternalResource nodeResource, StashResult result) {
        buildLogger.println("ERROR Failed to release resource " + nodeResource.getId());
        if (result != null) {
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR, AdminNotifier.OperationType.RELEASE,
                        build.getBuiltOn(), nodeResource, "Failed to release external resource from build: "
                            + build.getFullDisplayName() + " Status: " + result.getStatus() + ", Code: "
                            + result.getErrorCode() + ", Message: " + result.getMessage());
            buildLogger.println("\tStatus: " + result.getStatus()
                    + " Code: " + result.getErrorCode() + " Message: " + result.getMessage());
            logger.log(Level.SEVERE, "Failed to release resource [{0}] from build [{1}]:"
                    + " Status: {2}, Code: {3}, Message: {4}",
                    new Object[]{nodeResource.getId(), build.getFullDisplayName(),
                            result.getStatus(), result.getErrorCode(), result.getMessage(), });
        } else {
            AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR, AdminNotifier.OperationType.RELEASE,
                        build.getBuiltOn(), nodeResource, "Failed to release external resource  from build:"
                            + build.getFullDisplayName() + "No result!");
            logger.log(Level.SEVERE, "Failed to release resource [{0}] from build [{1}]: "
                    + "No Result!",
                    new Object[]{nodeResource.getId(), build.getFullDisplayName()});
        }
    }
}
