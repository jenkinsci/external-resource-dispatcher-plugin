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

import com.sonyericsson.hudson.plugins.metadata.model.MetadataBuildAction;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataParent;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeStructureUtil;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ReservedExternalResourceAction;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.AbstractResourceSelection;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection.StringResourceSelection;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.Builder;
import hudson.tasks.Mailer;
import hudson.tasks.Shell;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jvnet.hudson.test.HudsonTestCase;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Future;

//CS IGNORE MagicNumber FOR NEXT 300 LINES. REASON: TestData.

/**
 * Hudson Tests for {@link ExternalResourceQueueTaskDispatcher}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class ExternalResourceQueueTaskDispatcherHudsonTest extends HudsonTestCase {

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

    /**
     * Tests {@link ExternalResourceQueueTaskDispatcher#canTake(hudson.model.Node, hudson.model.Queue.BuildableItem)}
     * that a resource is reserved as it should.
     *
     * @throws Exception if so.
     */
    public void testCanTakeReserved() throws Exception {

        FreeStyleProject project = this.createFreeStyleProject("testProject");
        project.setAssignedLabel(new LabelAtom("TEST"));
        project.getBuildersList().add(new Shell("sleep 2"));
        //project = this.configRoundtrip(project);
        //TODO an active selection criteria
        AbstractResourceSelection selection = new StringResourceSelection("is.matching", "yes");
        boolean selectionEnabled = true;
        project.addProperty(new SelectionCriteria(selectionEnabled, Collections.singletonList(selection)));

        MetadataParent parent = resource.getParent();
        resource = Mockito.spy(resource);
        parent.setChild(parent.indexOf(resource.getName()), resource);
        long start = System.currentTimeMillis();

        Future<FreeStyleBuild> future = project.scheduleBuild2(0, new Cause.UserCause());
        StashInfo reservation = null;
        while (System.currentTimeMillis() - start < 10000 && !future.isDone()) {
            reservation = resource.getReserved();
            if (reservation == null) {
                Thread.sleep(500);
            } else {
                break;
            }
        }

        Mockito.verify(resource).setReserved(Mockito.argThat(new ValidReservation(project.getName())));
        ReservedExternalResourceAction action = future.get().getAction(ReservedExternalResourceAction.class);
        assertNotNull(action);
    }
    /**
     * Tests that a resource is locked as it should.
     *
     * @throws Exception if so.
     */
    public void testLock() throws Exception {
        FreeStyleProject project = this.createFreeStyleProject("testProject");
        project.setAssignedLabel(new LabelAtom("TEST"));
        AbstractResourceSelection selection = new StringResourceSelection("is.matching", "yes");
        boolean selectionEnabled = true;
        project.addProperty(new SelectionCriteria(selectionEnabled, Collections.singletonList(selection)));
        LockBuilder builder = new LockBuilder();
        project.getBuildersList().add(builder);
        FreeStyleBuild build = this.buildAndAssertSuccess(project);
        assertNotNull(builder.builderResource);
        assertEquals(builder.builderResource.getId(), resource.getId());
        assertEquals(builder.stashedBy, build.getUrl());
    }

    /**
     * Tests that selection disabled makes the build go through even though
     * the selection criteria doesn't match any phone.
     *
     * @throws Exception if so.
     */
    public void testSelectionNotEnabled() throws Exception {
        FreeStyleProject project = this.createFreeStyleProject("testProject");
        project.setAssignedLabel(new LabelAtom("TEST"));
        AbstractResourceSelection selection = new StringResourceSelection("is.matching", "no");
        boolean selectionEnabled = false;
        project.addProperty(new SelectionCriteria(selectionEnabled, Collections.singletonList(selection)));
        project.getBuildersList().add(new Shell("sleep 2"));
        FreeStyleBuild build = this.buildAndAssertSuccess(project);
    }

    /**
     * Builder that makes sure that it has a locked resource during the build.
     */
    protected static class LockBuilder extends Builder {
        private ExternalResource builderResource;
        private String stashedBy;
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            MetadataBuildAction action = build.getAction(MetadataBuildAction.class);
            if (action == null) {
                listener.error("No MetadataBuildAction in the build.");
                return false;
            }
            MetadataValue value = TreeStructureUtil.getPath(action, Constants.getBuildLockedResourcePath());
            if (value == null || !(value instanceof ExternalResource)) {
                listener.error("No locked resource in the build.");
                return false;
            }
            builderResource = (ExternalResource)value;
            stashedBy = builderResource.getLocked().getStashedBy();
         return true;
        }
    }

    /**
     * the Matcher class for valid reservation.
     * @author Zhang Leimeng
     */
    static class ValidReservation extends BaseMatcher<StashInfo> {
        String stashedByContains;

        /**
         * standard constructor.
         * @param stashedByContains the stash info which should be contained.
         */
        ValidReservation(String stashedByContains) {
            this.stashedByContains = stashedByContains;
        }

        @Override
        public boolean matches(Object o) {
            if (o != null && o instanceof StashInfo) {
                StashInfo info = (StashInfo)o;
                if (info.getStashedBy().contains(stashedByContains)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Valid Reservation");
        }

    }
}
