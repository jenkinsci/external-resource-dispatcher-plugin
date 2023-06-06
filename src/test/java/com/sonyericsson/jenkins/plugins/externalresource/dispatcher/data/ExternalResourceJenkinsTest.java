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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data;

import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import com.sonyericsson.hudson.plugins.metadata.cli.HttpCliRootAction;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.DateMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.StringMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.SelectionCriteria;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.AbstractResourceSelection;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.StringResourceSelection;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.spec.TestUtils;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.Mailer;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.sonyericsson.hudson.plugins.metadata.Constants.REQUEST_ATTR_METADATA_CONTAINER;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link HudsonTestCase}s for {@link ExternalResource}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonymobile.com&gt;
 */
public class ExternalResourceJenkinsTest extends HudsonTestCase {
    private DumbSlave slave;
    private MetadataNodeProperty property;
    private ExternalResource resource;

    //CS IGNORE LineLength FOR NEXT 4 LINES. REASON: JavaDoc

    /**
     * Tests {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource.ExternalResourceDescriptor#getValueDescriptors(org.kohsuke.stapler.StaplerRequest)}.
     */
    public void testGetValueDescriptors() {
        MetadataNodeProperty.MetadataNodePropertyDescriptor nodeDescriptor =
                Hudson.getInstance().getDescriptorByType(MetadataNodeProperty.MetadataNodePropertyDescriptor.class);
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getAttribute(REQUEST_ATTR_METADATA_CONTAINER)).thenReturn(nodeDescriptor);

        ExternalResource.ExternalResourceDescriptor descriptor =
                Hudson.getInstance().getDescriptorByType(ExternalResource.ExternalResourceDescriptor.class);

        List<AbstractMetadataValue.AbstractMetaDataValueDescriptor> descriptors =
                descriptor.getValueDescriptors(request);

        boolean foundString = false;
        boolean foundDate = false;
        boolean foundNode = false;
        boolean foundExternalResource = false;

        for (AbstractMetadataValue.AbstractMetaDataValueDescriptor d : descriptors) {
            if (d instanceof StringMetadataValue.StringMetaDataValueDescriptor) {
                foundString = true;
            } else if (d instanceof DateMetadataValue.DateMetaDataValueDescriptor) {
                foundDate = true;
            } else if (d instanceof TreeNodeMetadataValue.TreeNodeMetaDataValueDescriptor) {
                foundNode = true;
            } else if (d instanceof ExternalResource.ExternalResourceDescriptor) {
                foundExternalResource = true;
            }
        }
        assertTrue(foundString);
        assertTrue(foundDate);
        assertTrue(foundNode);
        assertFalse(foundExternalResource);
    }

    /**
     * Tests that an {@link ExternalResource} is correctly replaced by the metadata update command. In essence checks
     * that {@link ExternalResource#replacementOf(com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue)}
     * is correctly implemented.
     *
     * @throws Exception if so.
     */
    public void testReplacedWithEnabled() throws Exception {
        setUpSlave();
        resource.doEnable(false);
        String stashedBy = "theCoolBuild/1";
        String key = "oneKeyToRuleThemAll";
        resource.setLocked(new StashInfo(StashInfo.StashType.INTERNAL, stashedBy, null, key));

        ExternalResource nres = new ExternalResource("TestDevice", "description", "1", true,
                new LinkedList<MetadataValue>());
        String notAnyMore = "notAnyMore";
        String value = "value";
        TreeStructureUtil.addValue(nres, notAnyMore, "description", "is", "matching");
        TreeStructureUtil.addValue(nres, value, "description", "some", "other");
        TreeNodeMetadataValue tree = TreeStructureUtil.createPath(nres, "attached-devices", "test");
        JSONObject json = tree.toJson();
        String jsonString = json.toString();

        StaplerRequest request = mock(StaplerRequest.class);
        StaplerResponse response = mock(StaplerResponse.class);
        when(request.getParameter("data")).thenReturn(jsonString);
        when(request.getParameter("node")).thenReturn(slave.getNodeName());
        when(request.getParameter("replace")).thenReturn("true");
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(out);

        HttpCliRootAction metadataCliAction = TestUtils.getHttpCliRootAction();
        assertNotNull(metadataCliAction);

        metadataCliAction.doUpdate(request, response);

        ExternalResource replacedResource = (ExternalResource)TreeStructureUtil.getPath(property,
                "attached-devices", "test", "TestDevice");
        assertNotNull(replacedResource);
        assertNotNull(replacedResource.getLocked());
        assertEquals(stashedBy, replacedResource.getLocked().getStashedBy());
        assertEquals(key, replacedResource.getLocked().getKey());
        assertTrue(replacedResource.isEnabled());
        assertEquals(notAnyMore, TreeStructureUtil.getPath(replacedResource, "is", "matching").getValue());
        assertEquals(value, TreeStructureUtil.getPath(replacedResource, "some", "other").getValue());
        assertEquals(2, replacedResource.getChildren().size());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("type", "ok");
        expectedJson.put("errorCode", 0);
        expectedJson.put("message", "OK");

        verify(out).print(eq(expectedJson.toString()));
    }

    /**
     * Tests that the environment variables for the locked resource is available to the build.
     *
     * @throws Exception if so.
     */
    public void testLockedResourceEnvironment() throws Exception {
        setUpSlave();
        TreeStructureUtil.addValue(resource, "USB", "the type of connector", "connector", "type");
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedLabel(new LabelAtom("TEST"));
        StringResourceSelection selection = new StringResourceSelection("is.matching", "yes");
        List<AbstractResourceSelection> list = new LinkedList<AbstractResourceSelection>();
        list.add(selection);
        SelectionCriteria selectionCriteria = new SelectionCriteria(true, list);
        project.addProperty(selectionCriteria);

        final HashMap<Object, String> environment = new HashMap<Object, String>();

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                EnvVars vars = build.getEnvironment(listener);
                environment.putAll(vars);
                return true;
            }
        });

        FreeStyleBuild build = this.buildAndAssertSuccess(project);
        assertNotNull(build);

        assertEquals("USB", environment.get("MD_EXTERNAL_RESOURCES_LOCKED_CONNECTOR_TYPE"));
        assertEquals("yes", environment.get("MD_EXTERNAL_RESOURCES_LOCKED_IS_MATCHING"));
        assertEquals(resource.getId(), environment.get("MD_EXTERNAL_RESOURCES_LOCKED_ID"));
    }

    /**
     * Setup method for some of the tests. It creates a slave with an attached external resource.
     *
     * @throws Exception if so.
     */
    private void setUpSlave() throws Exception {
        slave = this.createOnlineSlave(new LabelAtom("TEST"));
        property = new MetadataNodeProperty((new LinkedList<MetadataValue>()));
        slave.getNodeProperties().add(property);
        resource = new ExternalResource("TestDevice", "description", "1", null,
                new LinkedList<MetadataValue>());
        TreeStructureUtil.addValue(resource, "yes", "description", "is", "matching");
        TreeStructureUtil.addValue(property, resource, "attached-devices", "test");
        Mailer.descriptor().setHudsonUrl(this.getURL().toString());
    }

    /**
     * Tests that the Disable button is visible on the node page.
     *
     * @throws Exception if so.
     */
    public void testCanEnableDisableOnNode() throws Exception {
        slave = this.createOnlineSlave();
        property = new MetadataNodeProperty();
        ExternalResource value = new ExternalResource("device1", "d1");
        TreeStructureUtil.addValue(value, "One", "The product version", "product", "version");
        ExternalResourceTreeNode tn = new ExternalResourceTreeNode("attached-devices");
        tn.addChild(value);
        TreeStructureUtil.addValue(property, tn, false, "external");
        slave.getNodeProperties().add(property);

        WebClient web = createWebClient();
        HtmlPage page = web.getPage(slave);
        DomNode text = getTextElement(page.getDocumentElement(), "external.attached-devices.device1");
        assertNotNull("Expected to find the device's name on the page", text);
        HtmlElement el = (HtmlElement)text.getParentNode();
        List<HtmlElement> buttons = el.getHtmlElementsByTagName("button");
        assertEquals(1, buttons.size());
        assertEquals("Disable", buttons.get(0).getFirstChild().asText());
    }

    /**
     * Tests that the Disable button is not on the build metadata page.
     *
     * @throws Exception if so.
     */
    public void testCantEnableDisableOnBuild() throws Exception {
        this.configRoundtrip();
        slave = this.createOnlineSlave();
        property = new MetadataNodeProperty();
        ExternalResource value = new ExternalResource("device1", "d1");
        TreeStructureUtil.addValue(value, "One", "The product version", "product", "version");
        ExternalResourceTreeNode tn = new ExternalResourceTreeNode("attached-devices");
        tn.addChild(value);
        TreeStructureUtil.addValue(property, tn, false, "external");
        slave.getNodeProperties().add(property);

        FreeStyleProject project = this.createFreeStyleProject();
        StringResourceSelection selection = new StringResourceSelection("product.version", "One");
        LinkedList<AbstractResourceSelection> list = new LinkedList<AbstractResourceSelection>();
        list.add(selection);
        project.addProperty(new SelectionCriteria(true, list));
        FreeStyleBuild build = this.buildAndAssertSuccess(project);


        WebClient web = createWebClient();
        HtmlPage page = web.getPage(build, "metadata");
        DomNode text = getTextElement(page.getDocumentElement(), "external-resources.locked");
        assertNotNull("Expected to find the device's name on the page", text);
        HtmlElement el = (HtmlElement)text.getParentNode();
        List<HtmlElement> buttons = el.getHtmlElementsByTagName("button");
        assertEquals("Expected no buttons on the resource row!", 0, buttons.size());
    }

    /**
     * Finds the DomNode beneath parent that has no children
     * and the provided text is returned from {@link DomNode#asText()}.
     *
     * @param parent the base parent.
     * @param needle the text to find.
     * @return the node that is the text or null if nothing was found.
     */
    private DomNode getTextElement(DomNode parent, String needle) {
        DomNodeList<DomNode> nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            DomNode node = nodes.get(i);
            if (node.hasChildNodes()) {
                DomNode text = getTextElement(node, needle);
                if (text != null) {
                    return text;
                }
            } else  {
                if (needle.equals(node.asText())) {
                    return node;
                }
            }
        }
        return null;
    }
}
