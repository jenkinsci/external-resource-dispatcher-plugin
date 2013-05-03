/*
 *  The MIT License
 *
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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers;

import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.cli.ErCliUtils;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import hudson.Extension;
import hudson.model.Node;
import hudson.triggers.Trigger;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager that does nothing.
 *
 * @author Robert Sandell &lt;robert.sandell@sonymobile.com&gt;
 */
@Extension
public class NoopExternalResourceManager extends ExternalResourceManager {

    @Override
    public String getDisplayName() {
        return Messages.NoopExternalResourceManager_DisplayName();
    }

    /**
     * The result that will be returned for every operation.
     */
    protected final StashResult okResult = new StashResult("noop", "noop");

    @Override
    public StashResult doReserve(Node node, ExternalResource resource, int seconds, String reservedBy) {
        Trigger.timer.schedule(new ReservationTimeoutTask(node.getNodeName(), resource.getId()),
                TimeUnit.SECONDS.toMillis(seconds));
        return okResult;
    }

    @Override
    public StashResult doLock(Node node, ExternalResource resource, String key, String lockedBy) {
        return okResult;
    }

    @Override
    public StashResult doRelease(Node node, ExternalResource resource, String key, String releasedBy) {
        return okResult;
    }

    @Override
    public boolean isExternalLockingOk() {
        return false;
    }

    @Override
    public void updateMetadata(AbstractMetadataValue value) {
        //nothing to do here.
    }

    /**
     * TimerTask to schedule when a Noop reservation times out.
     */
    static class ReservationTimeoutTask extends TimerTask {
        /**
         * The logger
         */
        private static final Logger logger = Logger.getLogger(ReservationTimeoutTask.class.getName());
        private String node;
        private String id;

        /**
         * standard Constructor.
         *
         * @param node the name of the node.
         * @param id   the id of the resource.
         */
        ReservationTimeoutTask(String node, String id) {
            this.node = node;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                logger.fine("Reservation timeout.");
                ExternalResource er = ErCliUtils.findExternalResource(node, id);
                er.doExpireReservation();
            } catch (CmdLineException e) {
                logger.log(Level.WARNING, "Failed to timeout a reservation of " + id + " on node " + node + "!", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to save the new reservation state to disk!", e);
            }
        }
    }
}
