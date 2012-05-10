/*
 *  The MIT License
 *
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


import com.sonyericsson.hudson.plugins.metadata.model.JsonUtils;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataContainer;
import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl;
import com.sonyericsson.jenkins.plugins.externalresource.
        dispatcher.utils.resourcemanagers.DefaultExternalResourceManager;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.ExternalResourceManager;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.LinkedList;
import java.util.List;

/**
 * TreeNodeMetadataValue subclass which can contain ExternalResources.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class ExternalResourceTreeNode extends TreeNodeMetadataValue {

    private ExternalResourceManager manager;


    /**
     * Standard DataBoundConstructor.
     * @param name the name.
     * @param description the description.
     * @param children the list of children of this ExternalResourceTreeNode.
     * @param managerName the class name of the chosen ExternalResourceManager.
     * @param exposedToEnvironment if this value should be exposed to the build as an environment variable.
     */
    @DataBoundConstructor
    public ExternalResourceTreeNode(String name, String description,
                                    List<MetadataValue> children,
                                    String managerName,
                                    boolean exposedToEnvironment) {
        super(name, description, children, exposedToEnvironment);
        ExternalResourceManager dynamic =
                PluginImpl.getInstance().getAvailableExternalResourceManagers().getDynamic(managerName);
        this.manager = dynamic;

    }

    /**
     * Standard constructor.
     * @param name the name.
     * @param description the description.
     * @param children the list of children.
     * @param exposedToEnvironment if this value should be exposed to the build as an environment variable.
     */
    public ExternalResourceTreeNode(String name, String description, List<MetadataValue> children,
                                    boolean exposedToEnvironment) {
        super(name, description, children, exposedToEnvironment);
    }

    /**
     * Standard constructor.
     * @param name the name.
     * @param description the description.
     * @param children the list of children.
     */
    public ExternalResourceTreeNode(String name, String description, List<MetadataValue> children) {
        super(name, description, children);
    }

    /**
     * Standard constructor.
     * @param name the name.
     * @param description the description.
     */
    public ExternalResourceTreeNode(String name, String description) {
        super(name, description);
    }

    /**
     * Standard constructor.
     * @param name the name.
     * @param children the list of children.
     */
    public ExternalResourceTreeNode(String name, List<MetadataValue> children) {
        super(name, children);
    }

    /**
     * Standard constructor.
     * @param name the name.
     */
    public ExternalResourceTreeNode(String name) {
        super(name);
    }

    /**
     * Standard getter for the ExternalResourceManager.
     * @return the ExternalResourceManager.
     */
    public ExternalResourceManager getManager() {
        return manager;
    }

    @Override
    public Descriptor<AbstractMetadataValue> getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(ExternalResourceTreeNodeDescriptor.class);
    }

    /**
     * Returns the ExternalResourceManager if it is not null, otherwise returns the generic ExternalResourceManager
     * of the plugin.
     * @return the ExternalResourceManager.
     */
    public ExternalResourceManager findManager() {
        if (manager != null) {
            return manager;
        } else {
            return PluginImpl.getInstance().getManager();
        }
    }

    /**
     * Descriptor for {@link ExternalResourceTreeNode}s.
     */
    @Extension
    public static class ExternalResourceTreeNodeDescriptor extends AbstractMetaDataValueDescriptor {

        @Override
        public String getJsonType() {
            return "externalresource-treenode";
        }

        @Override
        public MetadataValue fromJson(JSONObject json, MetadataContainer<MetadataValue> container)
                throws JsonUtils.ParseException {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "External Resource TreeNode";
        }

        /**
            * Convenience method for views.
            * @return the list of available ExternalResourceManagers.
            */
        public List<ExternalResourceManager> getAvailableExternalResourceManagers() {
            List<ExternalResourceManager> list = new LinkedList<ExternalResourceManager>();
            ExtensionList<ExternalResourceManager> availableExternalResourceManagers =
                    PluginImpl.getInstance().getAvailableExternalResourceManagers();
            for (ExternalResourceManager manager : availableExternalResourceManagers) {
                list.add(manager);
            }
            list.add(new DefaultExternalResourceManager());
            return list;
        }

        /**
            * Returns all the registered meta data descriptors. For use in a hetero-list.
            *
            * @param request the current request.
            * @return the descriptors.
            */
        public List<AbstractMetaDataValueDescriptor> getValueDescriptors(StaplerRequest request) {
            List<AbstractMetaDataValueDescriptor> list = new LinkedList<AbstractMetaDataValueDescriptor>();
            ExtensionList<ExternalResource.ExternalResourceDescriptor> extensionList =
                    Hudson.getInstance().getExtensionList(ExternalResource.ExternalResourceDescriptor.class);
            list.addAll(extensionList);
            return list;
        }

        /**
         * Convenience method for views.
         * @return the translable display name of the default manager.
         */
        public String getDefaultManagerDisplayName() {
            return Messages.DefaultExternalResourceManager_DisplayName();
        }
    }
}
