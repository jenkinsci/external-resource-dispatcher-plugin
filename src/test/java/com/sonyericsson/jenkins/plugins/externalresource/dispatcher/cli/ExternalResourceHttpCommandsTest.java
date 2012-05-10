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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.cli;

import com.sonyericsson.hudson.plugins.metadata.cli.CliUtils;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.MockUtils;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.
        DeviceMonitorExternalResourceManager;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.ExternalResourceManager;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.NoopExternalResourceManager;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerResponse;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import java.util.Collections;
import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ExternalResourceHttpCommands}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CliUtils.class, Hudson.class, ACL.class, PluginImpl.class })
public class ExternalResourceHttpCommandsTest {

    private MetadataNodeProperty container;
    private StaplerResponse response;
    private ServletOutputStream out;
    private ExternalResourceHttpCommands action;
    private Node node;

    /**
     * Do some mocking for all tests.
     *
     * @throws Exception if so and fail.
     */
    @Before
    public void prepareSomeStuff() throws Exception {
        Hudson hudson = MockUtils.mockHudson();
        MockUtils.mockMetadataValueDescriptors(hudson);

        PowerMockito.mockStatic(PluginImpl.class);
        PluginImpl pluginImpl = mock(PluginImpl.class);

        PowerMockito.when(PluginImpl.getInstance()).thenReturn(pluginImpl);
        ExternalResourceManager manager = mock(DeviceMonitorExternalResourceManager.class);
        when(manager.isExternalLockingOk()).thenReturn(true);
        when(pluginImpl.getManager()).thenReturn(manager);
        container = new MetadataNodeProperty(new LinkedList<MetadataValue>());
        container = spy(container);
        ACL acl = PowerMockito.mock(ACL.class);
        when(container.getACL()).thenReturn(acl);

        node = mock(Node.class);
        when(hudson.getNode(anyString())).thenReturn(node);
        DescribableList list = mock(DescribableList.class);
        when(node.getNodeProperties()).thenReturn(list);
        when(list.get(MetadataNodeProperty.class)).thenReturn(container);

        PowerMockito.mockStatic(CliUtils.class);

        response = mock(StaplerResponse.class);
        out = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(out);

        action = new ExternalResourceHttpCommands();

    }

    /**
     * Happy test for {@link ExternalResourceHttpCommands#doEnable(String, String, org.kohsuke.stapler.StaplerResponse)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoEnable() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                false, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");

        action.doEnable("testNode", id, response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertTrue(resource.isEnabled());
        verify(container).save();

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Happy test for
     * {@link ExternalResourceHttpCommands#doDisable(String, String, org.kohsuke.stapler.StaplerResponse)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoDisable() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                true, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");

        action.doDisable("testNode", id, response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertFalse(resource.isEnabled());
        verify(container).save();

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Happy test for
     * {@link ExternalResourceHttpCommands#doExpireReservation(String, String, org.kohsuke.stapler.StaplerResponse)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoExpire() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                true, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");

        action.doExpireReservation("testNode", id, response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertNull(resource.getReserved());
        verify(container).save();

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Happy test for {@link ExternalResourceHttpCommands#
     * doLockResource(String, String, String, String, org.kohsuke.stapler.StaplerResponse)} .
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoLockAndReleaseResource() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                false, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");
        JSONObject clientInfo = new JSONObject();
        clientInfo.put("id", Hudson.getInstance().getRootUrl());
        clientInfo.put("url", "");

        action.doLockResource("testNode", id, "ILockedIt", clientInfo.toString(), response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertNotNull(resource.getLocked());
        assertEquals("ILockedIt", resource.getLocked().getStashedBy());

        action.doReleaseResource("testNode", id, clientInfo.toString(), response);
        assertNull(resource.getLocked());
        verify(container, times(2)).save();
        verify(out, times(2)).print(eq(expectedJson.toString()));
    }

    /**
     * Happy test for
     * {@link ExternalResourceHttpCommands#
     * doReserveResource(String, String, String, String, org.kohsuke.stapler.StaplerResponse)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoReserveResource() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                false, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");

        JSONObject clientInfo = new JSONObject();
        clientInfo.put("id", Hudson.getInstance().getRootUrl());
        clientInfo.put("url", "");
        action.doReserveResource("testNode", id, "IReservedIt", clientInfo.toString(), response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertNotNull(resource.getReserved());
        assertEquals("IReservedIt", resource.getReserved().getStashedBy());

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Happy test for when clientInfo does not contain the correct key-value pairs.
     * It should be possible to reserve anyway, clientInfo should just prevent circular calls from
     * Jenkins to happen.
     * @throws Exception if so.
     */
    @Test
    public void testDoReserveResourceWithBadClientInfo() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                false, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");

        JSONObject clientInfo = new JSONObject();
        clientInfo.put("something", "Hello");
        clientInfo.put("somethingelse", ".");
        action.doReserveResource("testNode", id, "IReservedIt", clientInfo.toString(), response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertNotNull(resource.getReserved());
        assertEquals("IReservedIt", resource.getReserved().getStashedBy());

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Happy test for when clientInfo is null.
     * It should be possible to reserve anyway, clientInfo should just prevent circular calls from
     * Jenkins to happen.
     *
     * @throws Exception if so.
     */
    @Test
    public void testDoReserveResourceWithNullClientInfo() throws Exception {
        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                false, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");

        action.doReserveResource("testNode", id, "IReservedIt", null, response);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        assertNotNull(resource.getReserved());
        assertEquals("IReservedIt", resource.getReserved().getStashedBy());

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Tests that no lock can be done when using a NoopManager.
     * @throws Exception if so.
     */
    @Test(expected = IllegalStateException.class)
    public void testDoLockWithNoopManager() throws Exception {
        PowerMockito.mockStatic(PluginImpl.class);
        PluginImpl pluginImpl = mock(PluginImpl.class);

        PowerMockito.when(PluginImpl.getInstance()).thenReturn(pluginImpl);
        ExternalResourceManager manager = mock(NoopExternalResourceManager.class);
        when(pluginImpl.getManager()).thenReturn(manager);

        String id = "12345678";
        ExternalResource resource = new ExternalResource("Temp", "Temp", id,
                false, Collections.<MetadataValue>emptyList());
        TreeStructureUtil.addValue(container, resource, "test", "path");
        JSONObject clientInfo = new JSONObject();
        clientInfo.put("id", Hudson.getInstance().getRootUrl());
        clientInfo.put("url", "");
        action.doLockResource("testNode", id, "IReservedIt", clientInfo.toString(), response);
    }
}
