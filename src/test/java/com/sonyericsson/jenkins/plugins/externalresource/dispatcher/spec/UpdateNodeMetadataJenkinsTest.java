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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.spec;

import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.cli.ErCliUtils;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.Mailer;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletOutputStream;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specification tests regarding the metadata update command on nodes with
 * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource}s.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class UpdateNodeMetadataJenkinsTest extends HudsonTestCase {

    //CS IGNORE MagicNumber FOR NEXT 200 LINES. REASON: Test Data.

    private DumbSlave slave;
    private MetadataNodeProperty property;
    private ExternalResource resource;

    /**
     * Tests updating an {@link ExternalResource} via
     * {@link com.sonyericsson.hudson.plugins.metadata.cli.HttpCliRootAction#doUpdate(org.kohsuke.stapler.StaplerRequest,
     * org.kohsuke.stapler.StaplerResponse)}. With two properties that should be left intact, one exsisting that should
     * be changed and two new properties on different places in the tree. Also checking that there are no duplicates.
     *
     * @throws Exception if so.
     */
    public void testUpdateReplaceExistingAndNew() throws Exception {
        TreeStructureUtil.addValue(resource, "Prototype-1", "description", "hw", "version");
        TreeStructureUtil.addValue(resource, "X-Files", "description", "project");
        resource.doEnable(false);

        String resourceName = resource.getName();
        String resourceId = resource.getId();
        ExternalResource toUpdateResource = new ExternalResource(resourceName,
                resource.getDescription(), resourceId, true, null);
        //Change something
        TreeStructureUtil.addValue(toUpdateResource, "X-Prototype-2", "description", "hw", "version");
        //Add something new
        TreeStructureUtil.addValue(toUpdateResource, "Conductor", "description", "hw", "type");
        TreeStructureUtil.addValue(toUpdateResource, "High", "description", "rating", "availability");

        TreeNodeMetadataValue path = TreeStructureUtil.createPath(toUpdateResource, "attached-resources", "test");
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getParameter("node")).thenReturn(slave.getNodeName());
        when(request.getParameter("data")).thenReturn(path.toJson().toString());
        when(request.getParameter("replace")).thenReturn("true");
        StaplerResponse response = mock(StaplerResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(out);
        TestUtils.getHttpCliRootAction().doUpdate(request, response);

        ExternalResource externalResource = ErCliUtils.findExternalResource(slave.getNodeName(), resourceId);
        assertNotNull(externalResource);
        assertEquals("attached-resources.test." + resourceName, externalResource.getFullName());
        assertEquals("X-Prototype-2", TreeStructureUtil.getPath(externalResource, "hw", "version").getValue());
        assertEquals("X-Files", TreeStructureUtil.getPath(externalResource, "project").getValue());
        assertEquals("Conductor", TreeStructureUtil.getPath(externalResource, "hw", "type").getValue());
        assertEquals("High", TreeStructureUtil.getPath(externalResource, "rating", "availability").getValue());
        assertEquals("yes", TreeStructureUtil.getPath(externalResource, "is", "matching").getValue());
        assertEquals(4, externalResource.getChildren().size());
        assertEquals(2, ((List)TreeStructureUtil.getPath(externalResource, "hw").getValue()).size());
        assertEquals(1, ((List)TreeStructureUtil.getPath(externalResource, "rating").getValue()).size());
        assertEquals(1, ((List)TreeStructureUtil.getPath(externalResource, "is").getValue()).size());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        slave = this.createOnlineSlave(new LabelAtom("TEST"));
        property = new MetadataNodeProperty((new LinkedList<MetadataValue>()));
        slave.getNodeProperties().add(property);
        resource = new ExternalResource("TestDevice", "description", "1", null,
                new LinkedList<MetadataValue>());
        TreeStructureUtil.addValue(resource, "yes", "description", "is", "matching");
        TreeStructureUtil.addValue(property, resource, "attached-resources", "test");
        Mailer.descriptor().setHudsonUrl(this.getURL().toString());
    }
}
