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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.when;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.MockUtils;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashResult.Status;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager.
        DeviceMonitorExternalResourceManager.RpcResult;

/**
 * The unit test for the external resource manager.
 *
 * @author Zhang Leimeng
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonRpcUtil.class, Node.class, Hudson.class })
public class ExternalResourceManagerTest {

    /**
     * the method name of reserve.
     */
    private static final String RESERVE_METHOD = "DeviceMonitor.Devices.Reserve";

    /**
     * the method of lock.
     */
    private static final String LOCK_METHOD = "DeviceMonitor.Devices.Lock";

    /**
     * the method of release.
     */
    private static final String RELEASE_METHOD = "DeviceMonitor.Devices.Release";

    /**
     * test reserve method.
     */
    @Test
    public void testReserve() {

        Hudson mockHudson = MockUtils.mockHudson();
        when(mockHudson.getRootUrl()).thenReturn("jenkins");
        // the external resource we mocked.
        String externalResourceId = "id_1";
        String externalResourceName = "id_1";
        ExternalResource er = new ExternalResource(externalResourceName, externalResourceId);

        // mocked nodename, the host name used inside the monitor.
        String nodeName = "slave1";

        // the result we expected.
        String reserveKey = "348304849303";
        int code = 0;
        String message = "the device is reserved";

        RpcResult result = new RpcResult();
        long now = new Date().getTime();
        String isoTime = "China Beijing";
        result.setCode(code);
        result.setKey(reserveKey);
        result.setMessage(message);
        result.setStatus(Status.OK);
        result.setIsotime(isoTime);
        result.setTime(now);

        int time = (int)Math.random();
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("device", externalResourceId);
        paramMap.put("timeout", time);
        JSONObject clientInfo = new JSONObject();
                    clientInfo.put("id", "jenkins");
                    clientInfo.put("url", "me");
        paramMap.put("clientInfo", clientInfo);

        // mock for reserve.
        mockForOperation(result, new Object[]{paramMap}, nodeName, RESERVE_METHOD);

        // mock a node which has hostname.
        Node n = mockNode(nodeName);

        ExternalResourceManager rpcCallERM = new ExternalResourceManager.DeviceMonitorExternalResourceManager();

        StashResult sRes = rpcCallERM.reserve(n, er, time, "me");

        assertEquals(code, sRes.getErrorCode());
        assertEquals(message, sRes.getMessage());
        assertEquals(reserveKey, sRes.getKey());
        assertEquals(Status.OK , sRes.getStatus());
        assertTrue(sRes.isOk());
        assertEquals(isoTime, sRes.getLease().getSlaveIsoTime());
        assertNotNull(sRes.getLease().getServerTime());
    }

    /**
     * test lock method.
     */
    @Test
    public void testLock() {
        Hudson mockHudson = MockUtils.mockHudson();
        when(mockHudson.getRootUrl()).thenReturn("jenkins");
        // the external resource we mocked.
        String externalResourceId = "id_1";
        String externalResourceName = "id_1";
        ExternalResource er = new ExternalResource(externalResourceName, externalResourceId);

        // mocked nodename, the host name used inside the monitor.
        String nodeName = "slave1";

        // the result we expected.
        String reserveKey = "3483048493039334";
        int code = 0;
        String message = "the device is locked";

        RpcResult result = new RpcResult();
        result.setCode(code);
        result.setKey(reserveKey);
        result.setMessage(message);
        result.setStatus(Status.OK);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("device", externalResourceId);
        paramMap.put("key", reserveKey);
        JSONObject clientInfo = new JSONObject();
                    clientInfo.put("id", "jenkins");
                    clientInfo.put("url", "me");
        paramMap.put("clientInfo", clientInfo);

        // mock for reserve.
        mockForOperation(result, new Object[]{paramMap}, nodeName, LOCK_METHOD);

        // mock a node which has hostname.
        Node n = mockNode(nodeName);

        ExternalResourceManager rpcCallERM = new ExternalResourceManager.DeviceMonitorExternalResourceManager();

        StashResult sRes = rpcCallERM.lock(n, er, reserveKey, "me");

        assertEquals(code, sRes.getErrorCode());
        assertEquals(message, sRes.getMessage());
        assertEquals(Status.OK, sRes.getStatus());
        assertTrue(sRes.isOk());
        assertEquals(reserveKey, sRes.getKey());
    }

    /**
     * test release method.
     */
    @Test
    public void testRelease() {
        Hudson mockHudson = MockUtils.mockHudson();
        when(mockHudson.getRootUrl()).thenReturn("jenkins");
        // the external resource we mocked.
        String externalResourceId = "id_1";
        String externalResourceName = "id_1";
        ExternalResource er = new ExternalResource(externalResourceName, externalResourceId);

        // mocked nodename, the host name used inside the monitor.
        String nodeName = "slave1";

        // the result we expected.
        String reserveKey = "3483048493039334";
        int code = 0;
        String message = "the device is released";

        RpcResult result = new RpcResult();
        result.setCode(code);
        result.setKey(reserveKey);
        result.setMessage(message);
        result.setStatus(Status.OK);

        String key = "mockreservekey";
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("device", externalResourceId);
        paramMap.put("key", key);
        JSONObject clientInfo = new JSONObject();
                    clientInfo.put("id", "jenkins");
                    clientInfo.put("url", "me");
        paramMap.put("clientInfo", clientInfo);

        // mock for reserve.
        mockForOperation(result, new Object[] { paramMap }, nodeName, RELEASE_METHOD);

        // mock a node which has hostname.
        Node n = mockNode(nodeName);

        ExternalResourceManager rpcCallERM = new ExternalResourceManager.DeviceMonitorExternalResourceManager();

        StashResult sRes = rpcCallERM.release(n, er, key, "me");

        assertEquals(code, sRes.getErrorCode());
        assertEquals(Status.OK, sRes.getStatus());
        assertEquals(message, sRes.getMessage());
        assertTrue(sRes.isOk());
        assertEquals(reserveKey, sRes.getKey());
    }

    /**
     * mock job for the reserve method.
     * @param expectedOutput the expected {@link RpcResult}
     * @param expectedInput the expected input as Object[].
     * @param nodeName the expected node name.
     * @param  methodName the method name of the rpc call
     */
    private void mockForOperation(RpcResult expectedOutput, Object[] expectedInput, String nodeName, String methodName) {
        JsonRpcHttpClient mockRpcClient = PowerMockito.mock(JsonRpcHttpClient.class);
        try {
            when(mockRpcClient.invoke(methodName, expectedInput,
                            ExternalResourceManager.DeviceMonitorExternalResourceManager.RpcResult.class)).thenReturn(
                    expectedOutput);
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
        mockJsonRpcUtil(mockRpcClient, nodeName);
    }

    /**
     * mock the static JsonRpcUtil class to let the test work.
     * @param expectedClient the expected {@link JsonRpcHttpClient}.
     * @param nodeName the expected nodeName.
     */
    private void mockJsonRpcUtil(JsonRpcHttpClient expectedClient, String nodeName) {
        PowerMockito.mockStatic(JsonRpcUtil.class);
        when(JsonRpcUtil.createJsonRpcClient(MessageFormat.format("http://{0}:{1}/", nodeName, "8080"))).thenReturn(
                expectedClient);

        ObjectMapper om = JsonRpcUtil.customizeObjectMapper();
        when(JsonRpcUtil.customizeObjectMapper()).thenReturn(om);

        when(JsonRpcUtil.createJsonRpcClient(MessageFormat.format("http://{0}:{1}/", nodeName, "8080"), om))
                .thenReturn(expectedClient);
    }

    /**
     * mock a node which will return the expected node name.
     * @param nodeName the expected nodeName.
     * @return the {@link Node} which will return the nodeName.
     */
    private Node mockNode(String nodeName) {
        Computer com = PowerMockito.mock(Computer.class);
        try {
            when(com.getHostName()).thenReturn(nodeName);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        Node n = PowerMockito.mock(Node.class);
        PowerMockito.doReturn(com).when(n).toComputer();

        return n;
    }

}
