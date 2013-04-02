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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers;

import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import hudson.ExtensionPoint;
import hudson.model.Node;

/**
 * Manager for handling reservation of resources by external services. The Method Template pattern is used to allow for
 * sub-classes providing their own reservation functionality. For example the external resources on a slave might be 
 * managed by a daemon who handles locking and unlocking of the resources on the slave itself, and you extend this  
 * class to provide your own implementation of such a communication interface.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public abstract class ExternalResourceManager implements ExtensionPoint {

    /**
     * The name of the manager to show the admin.
     *
     * @return the name.
     */
    public abstract String getDisplayName();

    /**
     * Reserve the resource on the node. A reservation has a deadline, if the resource isn't locked until the lease
     * expires the service should unlock the resource so it can be used by another build. So if the {@link
     * com.sonyericsson.jenkins.plugins.externalresource.dispatcher.ExternalResourceQueueTaskDispatcher} reserves a
     * resource but another {@link hudson.model.queue.QueueTaskDispatcher} vetoes the build, the resource should not
     * be set to used for too long.
     *
     * @param node       the node to communicate with.
     * @param resource   the resource to reserve.
     * @param seconds    the number of seconds the lease should be.
     * @param reservedBy a String describing what reserved the resource.
     * @return the result.
     */
    public StashResult reserve(Node node, ExternalResource resource, int seconds, String reservedBy){
      StashResult result = doReserve(node, resource, seconds, reservedBy);

      if (result != null && result.isOk()) {
        resource.setReserved(new StashInfo(result, reservedBy));
      }

      return result;
    }


    /**
     * Implementation of the reservation mechanism itself. Sub-classes can call external sevices to do the actual locking 
     * if needed.
     * @param node       the node to communicate with.
     * @param resource   the resource to reserve.
     * @param seconds    the number of seconds the lease should be.
     * @param reservedBy a String describing what reserved the resource.
     * @return the result.
     */
    protected abstract StashResult doReserve(Node node, ExternalResource resource, int seconds, String reservedBy);


    /**
     * Locks the resource (permanently) until it is unlocked, no other build should be able to use this resource.
     *
     * @param node     the node holding the resource.
     * @param resource the resource to lock.
     * @param key      the key to be able to lock it (retained from
     *                 {@link #reserve(hudson.model.Node, ExternalResource, int, String)}).
     * @param lockedBy a String describing what locked the resource.
     * @return the result.
     */
    public StashResult lock(Node node, ExternalResource resource, String key, String lockedBy){
      StashResult result = doLock(node, resource, key, lockedBy);

      if (result != null && result.isOk()) {
        resource.setLocked(new StashInfo(result, lockedBy));
      }

      return result;
    }


    /**
     * Implementation of the locking mechanism itself. Sub-classes can call external sevices to do the actual locking
     * if needed.
     * @param node     the node holding the resource.
     * @param resource the resource to lock.
     * @param key      the key to be able to lock it (retained from
     *                 {@link #reserve(hudson.model.Node, ExternalResource, int, String)}).
     * @param lockedBy a String describing what locked the resource.
     * @return the result.
     */
    protected abstract StashResult doLock(Node node, ExternalResource resource, String key, String lockedBy);


    /**
     * Releases the resource, other builds can now use it.
     *
     * @param node       the node holding the resource.
     * @param resource   the resource to unlock.
     * @param key        the key to unlock the resource with (retained from a previous call to
     *                   {@link  #lock(hudson.model.Node,
     *                   com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource,
     *                   String, String)}.
     * @param releasedBy a String describing what released the resource.
     * @return the result.
     */
    public StashResult release(Node node, ExternalResource resource, String key, String releasedBy){
      StashResult result = doRelease(node, resource, key, releasedBy);

      if (result != null && result.isOk()) {
        resource.setReserved(null);
        resource.setLocked(null);
      }

      return result;
    }


    /**
     * Implementation of the releasing mechanism itself. Sub-classes can call external sevices to do the actual locking
     * if needed.
     * @param node       the node holding the resource.
     * @param resource   the resource to unlock.
     * @param key        the key to unlock the resource with (retained from a previous call to
     *                   {@link  #lock(hudson.model.Node,
     *                   com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource,
     *                   String, String)}.
     * @param releasedBy a String describing what released the resource.
     * @return the result.
     */
    protected abstract StashResult doRelease(Node node, ExternalResource resource, String key, String releasedBy);

    /**
     * Answers true if these operations are allowed using this ExternalResourceManager.
     *
     * @return true if allowed, false if not.
     */
    public abstract boolean isExternalLockingOk();

    /**
     * This method should be run when there is new Metadata that the ResourceManager should know of.
     *
     * @param value an AbstractMetadataValue to update
     */
    public abstract void updateMetadata(AbstractMetadataValue value);


}
