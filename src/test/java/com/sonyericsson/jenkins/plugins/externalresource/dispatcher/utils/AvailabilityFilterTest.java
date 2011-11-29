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
import com.sonyericsson.hudson.plugins.metadata.model.values.StringMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import hudson.model.Node;
import hudson.util.DescribableList;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AvailabilityFilter}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class AvailabilityFilterTest {

    //CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

    /**
     * Tests {@link AvailabilityFilter#getExternalResourcesList(hudson.model.Node)}.
     *
     * @throws Exception if so.
     */
    @Test
    public void testGetExternalResourcesList() throws Exception {
        //First create a structure.
        MetadataNodeProperty property = new MetadataNodeProperty(new LinkedList<MetadataValue>());
        TreeStructureUtil.addValue(property, "someValue", "aa", "aa"); //Just something that is not a resource
        ExternalResource resource = new ExternalResource("1", "1");
        TreeStructureUtil.addValue(resource, "1", "description", "product", "name");
        TreeStructureUtil.addValue(property, resource, "resources", "attached");
        resource = new ExternalResource("2", "2");
        TreeStructureUtil.addValue(resource, "2", "description", "product", "name");
        TreeStructureUtil.addValue(property, resource, "resources", "attached");
        resource = new ExternalResource("3", "3");
        TreeStructureUtil.addValue(resource, "3", "description", "product", "name");
        property.addChild(new StringMetadataValue("inTheWay", "value")); //Directly beneath the property
        property.addChild(resource); //Directly beneath the property

        Node node = mock(Node.class);
        DescribableList mockList = mock(DescribableList.class);
        when(mockList.get(MetadataNodeProperty.class)).thenReturn(property);
        when(node.getNodeProperties()).thenReturn(mockList);

        List<ExternalResource> list = AvailabilityFilter.getInstance().getExternalResourcesList(node);
        assertNotNull(list);
        assertEquals(3, list.size());
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;

        for (ExternalResource er : list) {
            if ("1".equals(er.getId())) {
                if (found1) {
                    fail("id: 1 found more than once!");
                }
                found1 = true;
            } else if ("2".equals(er.getId())) {
                if (found2) {
                    fail("id: 2 found more than once!");
                }
                found2 = true;
            } else if ("3".equals(er.getId())) {
                if (found3) {
                    fail("id: 3 found more than once!");
                }
                found3 = true;
            } else {
                fail("Found unexpected id: " + er.getId());
            }
        }

        assertTrue(found1);
        assertTrue(found2);
        assertTrue(found3);
    }
}
