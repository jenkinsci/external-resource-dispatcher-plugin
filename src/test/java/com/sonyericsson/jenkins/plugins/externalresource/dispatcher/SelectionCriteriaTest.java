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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher;

import java.util.LinkedList;
import java.util.List;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.AbstractResourceSelection;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.StringResourceSelection;
import junit.framework.Assert;
import org.junit.Test;

import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;

/**
 * Tests for {@link SelectionCriteria}.
 *
 * @author Ren Wei &lt;wei2.ren@sonyericsson.com&gt;
 */
public class SelectionCriteriaTest {
    /**
     * test GetMatchingResources. ExternalResource has several levels while StringResourceSelection contains
     * "." to represent levels
     */
    @Test
    public void testGetMatchingResources() {
        List<AbstractResourceSelection> resourceSelectionList = new LinkedList<AbstractResourceSelection>();
        AbstractResourceSelection selection1 = new StringResourceSelection("product.label.name", "Anzu");
        resourceSelectionList.add(selection1);
        AbstractResourceSelection selection2 = new StringResourceSelection("sim.operator", "Orange");
        resourceSelectionList.add(selection2);
        SelectionCriteria sc = new SelectionCriteria(resourceSelectionList);
        List<ExternalResource> availableResourceList = new LinkedList<ExternalResource>();
        ExternalResource er1 = new ExternalResource("er1", "1");
        //ExternalResource 1 with leaf value Anzu
        TreeStructureUtil.addValue(er1, "Anzu", "description", "product", "label", "name");
        TreeStructureUtil.addValue(er1, "Orange", "description", "sim", "operator");
        availableResourceList.add(er1);
        ExternalResource er2 = new ExternalResource("er2", "2");
        //ExternalResource 2 with leaf value Hallon
        TreeStructureUtil.addValue(er2, "Hallon", "description", "product", "label", "name");
        TreeStructureUtil.addValue(er2, "Orange", "description", "sim", "operator");
        availableResourceList.add(er2);
        List<ExternalResource> matchingResources = sc.getMatchingResources(availableResourceList);
        Assert.assertEquals(1, matchingResources.size());
        Assert.assertEquals("Anzu", TreeStructureUtil.getPath(matchingResources.get(0), "product", "label", "name")
                .getValue());
        Assert.assertEquals("Orange", TreeStructureUtil.getPath(matchingResources.get(0), "sim", "operator")
                .getValue());
    }
}
