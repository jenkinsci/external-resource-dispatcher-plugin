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

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.reflect.Whitebox;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Tests for {@link PluginImpl}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImplHudsonTest extends HudsonTestCase {

    /**
     * Tests that {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl#getManager()} returns
     * the correct default manager when no config is set.
     */
    public void testDefaultManager() {
        assertSame(PluginImpl.getNoopResourceManager(), PluginImpl.getInstance().getManager());
    }

    /**
     * Tests that the correct manager is set when it is configured.
     *
     * @throws Descriptor.FormException if so.
     * @throws IOException              if so.
     * @throws ServletException         if so.
     */
    public void testConfigureManager() throws Descriptor.FormException, IOException, ServletException {
        JSONObject config = new JSONObject();
        config.put(PluginImpl.FORM_NAME_RELEASE_KEY, "some_key");
        config.put(PluginImpl.FORM_NAME_RESERVE_TIME, PluginImpl.getInstance().getDefaultReserveTime());
        config.put(PluginImpl.FORM_NAME_MANAGER,
                ExternalResourceManager.DeviceMonitorExternalResourceManager.class.getName());
        config.put(PluginImpl.FORM_NAME_ADMIN_FILE, "/tmp/notify.csv");
        PluginImpl.getInstance().configure(null, config);

        ExternalResourceManager expected = Hudson.getInstance().getExtensionList(ExternalResourceManager.class)
                .get(ExternalResourceManager.DeviceMonitorExternalResourceManager.class);
        assertSame(expected, PluginImpl.getInstance().getManager());

        String expectedManagerClass = Whitebox.getInternalState(PluginImpl.getInstance(), "managerClass");
        assertEquals(expectedManagerClass, expected.getClass().getName());
    }
}
