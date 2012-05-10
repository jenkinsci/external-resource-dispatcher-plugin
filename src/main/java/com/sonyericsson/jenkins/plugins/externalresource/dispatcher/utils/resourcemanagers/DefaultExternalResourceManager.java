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
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import hudson.model.Node;

/**
 * The representation of a default ExternalResourceManager.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class DefaultExternalResourceManager extends ExternalResourceManager {

    @Override
    public String getDisplayName() {
        return Messages.DefaultExternalResourceManager_DisplayName();
    }

    @Override
    public StashResult reserve(Node node, ExternalResource resource, int seconds, String reservedBy) {

        return null;
    }

    @Override
    public StashResult lock(Node node, ExternalResource resource, String key, String lockedBy) {
        return null;
    }

    @Override
    public StashResult release(Node node, ExternalResource resource, String key, String releasedBy) {
        return null;
    }

    @Override
    public boolean isExternalLockingOk() {
        return false;
    }

    @Override
    public void updateMetadata(AbstractMetadataValue value) {
        //nothing to do here.
    }
}
