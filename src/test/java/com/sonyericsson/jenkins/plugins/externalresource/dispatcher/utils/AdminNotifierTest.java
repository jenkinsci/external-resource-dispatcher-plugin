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

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import hudson.Util;
import hudson.model.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * Tests for {@link AdminNotifier}.
 * @author Hu, Jack &lt;jack.hu@sonyericsson.com&gt;
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PluginImpl.class)
public class AdminNotifierTest {

    private Node node;

    /**
     * Create node and mock.
     */
    @Before
    public void setup() {
        node = mock(Node.class);
        String name = "cnbjlx1234";
        when(node.getDisplayName()).thenReturn(name);

        // mock for the PluginImpl
        PluginImpl mockPI = mock(PluginImpl.class);
        when(mockPI.getAdminNotifierFile()).thenReturn("/tmp/notify.csv");

        PowerMockito.mockStatic(PluginImpl.class);
        when(PluginImpl.getInstance()).thenReturn(mockPI);
    }

    /**
     * Test for {@link AdminNotifier#notify(AdminNotifier.MessageType, AdminNotifier.OperationType,
     * Node, ExternalResource, String)} .
     */
    @Test
    public void testNotify() {
        String externalResourceId = "123456789";
        String externalResourceName = "Aoba";
        ExternalResource er = new ExternalResource(externalResourceName, externalResourceId);
        String message = "test admin notifier..";
        String expectedMessage = MessageFormat.format("{0}, {1}, {2}, {3}, {4}",
                AdminNotifier.MessageType.ERROR.toString(), externalResourceId,
                AdminNotifier.OperationType.LOCK.toString(), node.getDisplayName(), message);
        AdminNotifier.getInstance().setAdminFile("/tmp/notify.csv");
        AdminNotifier.getInstance().notify(AdminNotifier.MessageType.ERROR,
                AdminNotifier.OperationType.LOCK, node, er, message);
        String lastLine = getLastLine("/tmp/notify.csv");
        assertEquals(expectedMessage, lastLine);
        try {
            cleanFile("/tmp/notify.csv");
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Get the last line of the file.
     *
     * @param fileName         admin notifier file name.
     * @return  last line of the file.
     */
    private String getLastLine(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        String lastLine = null;
        String str = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            while ((str = reader.readLine()) != null) {
                   lastLine = str;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    fail();
                }
            }
        }
        return lastLine;
    }

    /**
     * Get the last line of the file.
     *
     * @param fileName         admin notifier file name.
     * @throws IOException        throw IOException.
     */
    private void cleanFile(String fileName) throws IOException {
        File file = new File(fileName);
        Util.deleteRecursive(file);
    }

}
