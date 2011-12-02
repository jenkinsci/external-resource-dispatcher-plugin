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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils;

import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.Mailer;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.LinkedList;

//CS IGNORE LineLength FOR NEXT 6 LINES. REASON: JavaDoc.

/**
 * Tests for
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager.NoopExternalResourceManager}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class NoopExternalResourceManagerHudsonTest extends HudsonTestCase {

    //CS IGNORE MagicNumber FOR NEXT 200 LINES. REASON: Test Data.

    private DumbSlave slave;
    private MetadataNodeProperty property;
    private ExternalResource resource;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        slave = this.createOnlineSlave(new LabelAtom("TEST"));
        property = new MetadataNodeProperty((new LinkedList<MetadataValue>()));
        slave.getNodeProperties().add(property);
        resource = new ExternalResource("TestDevice", "description", "1", true,
                new LinkedList<MetadataValue>());
        TreeStructureUtil.addValue(resource, "yes", "description", "is", "matching");
        TreeStructureUtil.addValue(property, resource, "attached-devices", "test");
        Mailer.descriptor().setHudsonUrl(this.getURL().toString());
    }

    //CS IGNORE LineLength FOR NEXT 6 LINES. REASON: JavaDoc.
    /**
     * Tests
     * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager.NoopExternalResourceManager
     * #reserve(hudson.model.Node, com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource, int)}.
     * That a resource reservation times out after the specified interval.
     */
    public void testReserve() {
        StashResult result = PluginImpl.getNoopResourceManager().reserve(slave, resource, 1);
        resource.setReserved(new StashInfo(result, "me"));
        assertTrue(result.isOk());
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            fail();
        }
        assertNull(resource.getReserved());
    }
}
