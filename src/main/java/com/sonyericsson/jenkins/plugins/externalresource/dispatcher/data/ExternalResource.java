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

import com.sonyericsson.hudson.plugins.metadata.model.JsonUtils;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataContainer;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataParent;
import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.ACL;
import hudson.security.Permission;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.sonyericsson.hudson.plugins.metadata.Constants.REQUEST_ATTR_METADATA_CONTAINER;
import static com.sonyericsson.hudson.plugins.metadata.model.JsonUtils.CHILDREN;
import static com.sonyericsson.hudson.plugins.metadata.model.JsonUtils.DESCRIPTION;
import static com.sonyericsson.hudson.plugins.metadata.model.JsonUtils.EXPOSED;
import static com.sonyericsson.hudson.plugins.metadata.model.JsonUtils.GENERATED;
import static com.sonyericsson.hudson.plugins.metadata.model.JsonUtils.NAME;
import static com.sonyericsson.hudson.plugins.metadata.model.JsonUtils.checkRequiredJsonAttribute;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.JSON_ATTR_ENABLED;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.JSON_ATTR_ID;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.JSON_ATTR_LOCKED;
import static com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants.JSON_ATTR_RESERVED;

/**
 * Metadata type representing an external resource attached to a Node.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@XStreamAlias(Constants.SERIALIZATION_ALIAS_EXTERNAL_RESOURCE)
public class ExternalResource extends TreeNodeMetadataValue {

    /**
     * IllegalStateException message from some methods if they are called before a monitor has been set.
     */
    private static final String NO_RESOURCE_MONITOR_EXCEPTION_MSG =
            "No resource monitor is currently active, this operation is not permitted.";
    private String id;
    private StashInfo reserved;
    private StashInfo locked;
    /**
     * For access control purposes enabled can internally have 3 values; not set, true or false.
     * All logic related to enabled should handle "not set" as enabled.
     * @see #isEnabled()
     */
    private Boolean enabled;

    /**
     * Standard DataBound Constructor.
     *
     * @param name        the name to identify it amongst its siblings.
     * @param description description
     * @param id          The unique ID of the resource
     * @param enabled     if the resource is enabled or not.
     * @param children    associated metadata.
     * @see TreeNodeMetadataValue#TreeNodeMetadataValue(String, String, java.util.List)
     */
    @DataBoundConstructor
    public ExternalResource(String name, String description, String id, Boolean enabled, List<MetadataValue> children) {
        super(name, description, children);
        this.id = id;
        this.enabled = enabled;
    }

    /**
     * Standard Constructor.
     *
     * @param name        the name to identify it amongst its siblings.
     * @param description description
     * @param id          The unique ID of the resource
     * @param children    associated metadata.
     * @see TreeNodeMetadataValue#TreeNodeMetadataValue(String, String, java.util.List)
     */
    public ExternalResource(String name, String description, String id, List<MetadataValue> children) {
        super(name, description, children);
        this.id = id;
    }

    /**
     * Standard Constructor.
     *
     * @param name        the name to identify it amongst its siblings.
     * @param description description
     * @param id          The unique ID of the resource
     * @see TreeNodeMetadataValue#TreeNodeMetadataValue(String, String)
     */
    public ExternalResource(String name, String id, String description) {
        super(name, description);
        this.id = id;
    }

    /**
     * Standard Constructor.
     *
     * @param name     the name to identify it amongst its siblings.
     * @param id       The unique ID of the resource
     * @param children associated metadata.
     * @see TreeNodeMetadataValue#TreeNodeMetadataValue(String, List)
     */
    public ExternalResource(String name, String id, List<MetadataValue> children) {
        super(name, children);
        this.id = id;
    }

    /**
     * Standard Constructor.
     *
     * @param name the name to identify it amongst its siblings.
     * @param id   The unique ID of the resource
     * @see TreeNodeMetadataValue#TreeNodeMetadataValue(String)
     */
    public ExternalResource(String name, String id) {
        super(name);
        this.id = id;
    }


    /**
     * The unique ID of the resource.
     *
     * @return the id.
     */
    public String getId() {
        return id;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    /**
     * Information about the reservation status if the resource. Null indicating not reserved.
     *
     * @return the reservation status.
     */
    public StashInfo getReserved() {
        return reserved;
    }

    /**
     * Information about the reservation status if the resource. Null indicating not reserved.
     *
     * @param reserved the reservation status.
     */
    public void setReserved(StashInfo reserved) {
        this.reserved = reserved;
    }

    /**
     * Information about the lock status if the resource. Null indicating not locked.
     *
     * @return the lock status.
     */
    public StashInfo getLocked() {
        return locked;
    }

    /**
     * Information about the lock status if the resource. Null indicating not locked.
     *
     * @param locked the lock status.
     */
    public void setLocked(StashInfo locked) {
        this.locked = locked;
    }

    /**
     * If this resource is enabled (true) or disabled (false) on the node. A disabled resource won't be selected for
     * reservation by the {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter}.
     *
     * @return enabled or not.
     */
    public boolean isEnabled() {
        if (enabled == null) {
            return true;
        } else {
            return enabled;
        }
    }

    /**
     * If this resource is enabled (true) or disabled (false) on the node. A disabled resource won't be selected for
     * reservation by the {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.AvailabilityFilter}.
     * The idea is that instead of removing a resource when it is temporarily removed and potentially loosing all
     * configuration, a monitoring service could just disable it instead. Intended for internal calls and serialization.
     * For external "user calls" see {@link #doEnable(boolean)}.
     *
     * @param enabled enabled or not.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Enables this resource and save the Node's config. The method first checks if the user has the required
     * permissions. Called from javascript and CLI.
     *
     * @param enable true to enable, false to disable.
     * @throws IOException if so during save.
     * @see PluginImpl#ENABLE_DISABLE_EXTERNAL_RESOURCE
     * @see com.sonyericsson.hudson.plugins.metadata.model.MetadataContainer#save()
     * @see #setEnabled(boolean)
     */
    @JavaScriptMethod
    public synchronized void doEnable(boolean enable) throws IOException {
        getACL().checkPermission(PluginImpl.ENABLE_DISABLE_EXTERNAL_RESOURCE);
        setEnabled(enable);
        getContainer().save();
    }

    /**
     * Locks a resource.
     *
     * @param info the StashInfo containing the lock information.
     * @throws IOException if the container cannot be saved.
     */
    public synchronized void doLock(StashInfo info) throws IOException {
        if (!(PluginImpl.getInstance().getManager().isExternalLockingOk())) {
            throw new IllegalStateException(NO_RESOURCE_MONITOR_EXCEPTION_MSG);
        }
        getACL().checkPermission(PluginImpl.LOCK_RELEASE_EXTERNAL_RESOURCE);
        setLocked(info);
        setReserved(null);
        getContainer().save();
    }

    /**
     * Reserves a resource.
     *
     * @param info the StashInfo containing the reservation information.
     * @throws IOException if the container cannot be saved.
     */
    public synchronized void doReserve(StashInfo info) throws IOException {
        if (!(PluginImpl.getInstance().getManager().isExternalLockingOk())) {
            throw new IllegalStateException(NO_RESOURCE_MONITOR_EXCEPTION_MSG);
        }
        getACL().checkPermission(PluginImpl.LOCK_RELEASE_EXTERNAL_RESOURCE);
        setReserved(info);
        setLocked(null);
        getContainer().save();
    }

    /**
     * Releases a resource from its reservations and locks.
     *
     * @throws IOException if the container cannot be saved.
     */
    public synchronized void doRelease() throws IOException {
        if (!(PluginImpl.getInstance().getManager().isExternalLockingOk())) {
            throw new IllegalStateException(NO_RESOURCE_MONITOR_EXCEPTION_MSG);
        }
        getACL().checkPermission(PluginImpl.LOCK_RELEASE_EXTERNAL_RESOURCE);
        setLocked(null);
        setReserved(null);
        getContainer().save();
    }

    /**
     * Make this resource reservation expired and save the Node's config.
     *
     * @throws IOException if so during save.
     * @see com.sonyericsson.hudson.plugins.metadata.model.MetadataContainer#save()
     * @see #setReserved(com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.StashInfo)
     */
    public synchronized void doExpireReservation() throws IOException {
        setReserved(null);
        getContainer().save();
    }

    /**
     * If this resource is available or not. I.e. it has neither a {@link #getReserved()} nor a {@link #getLocked()}
     * set. Not counting if {@link #isEnabled()} is true or not.
     *
     * @return true if the resource is available to take.
     */
    public boolean isAvailable() {
        return getReserved() == null && getLocked() == null;
    }

    /**
     * Gives the container's ACL.
     *
     * @return the ACL of the container.
     *
     * @see #getContainer()
     * @see com.sonyericsson.hudson.plugins.metadata.model.MetadataContainer#getACL()
     */
    public synchronized ACL getACL() {
        return getContainer().getACL();
    }

    /**
     * Control method to see if the current user has the {@link PluginImpl#ENABLE_DISABLE_EXTERNAL_RESOURCE} permission
     * or not. Convenience method for easy invocation from Jelly.
     *
     * @return true if the current user has the required permission.
     *
     * @see #getACL()
     * @see ACL#hasPermission(hudson.security.Permission)
     */
    public boolean hasEnableDisablePermission() {
        return getACL().hasPermission(PluginImpl.ENABLE_DISABLE_EXTERNAL_RESOURCE);
    }

    /**
     * Control method to see if the resource can be enabled/disabled or not.
     * Checks {@link #hasEnableDisablePermission()} and that the container is a {@link MetadataNodeProperty}.
     * Otherwise the Enable/Disable button should not be shown.
     *
     * @return true if so.
     */
    @SuppressWarnings("unused")
    public boolean canEnableDisable() {
        if (hasEnableDisablePermission()) {
            MetadataContainer<MetadataValue> container = getContainer();
            return container != null && container instanceof MetadataNodeProperty;
        }
        return false;
    }

    /**
     * Searches up the parent hierarchy for the container.
     *
     * @return the container for this resource or null if something is wrong.
     */
    private synchronized MetadataContainer<MetadataValue> getContainer() {
        return getContainer(getParent());
    }

    /**
     * Searches up the parent hierarchy for the container, starting with the provided parent.
     *
     * @param parent the parent to check/recursively search.
     * @return the container or null.
     */
    private synchronized MetadataContainer<MetadataValue> getContainer(MetadataParent<MetadataValue> parent) {
        if (parent instanceof MetadataContainer) {
            return (MetadataContainer<MetadataValue>)parent;
        } else if (parent == null) {
            return null;
        } else {
            return getContainer(((MetadataValue)parent).getParent());
        }
    }

    @Override
    public Descriptor<AbstractMetadataValue> getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(ExternalResourceDescriptor.class);
    }

    @Override
    public void addEnvironmentVariables(EnvVars variables, boolean exposeAll) {
        super.addEnvironmentVariables(variables, exposeAll);
        if (isExposedToEnvironment() || exposeAll) {
            variables.put(getEnvironmentName()
                    + com.sonyericsson.hudson.plugins.metadata.Constants.ENVIRONMENT_SEPARATOR + "ID",
                    getId());
        }
    }

    @Override
    public boolean requiresReplacement() {
        return true;
    }

    @Override
    public void replacementOf(MetadataValue old) {
        super.replacementOf(old);
        if (old instanceof ExternalResource) {
            ExternalResource other = (ExternalResource)old;
            if (reserved == null) {
                reserved = other.reserved;
            }
            if (locked == null) {
                locked = other.locked;
            }
            if (enabled == null) {
                enabled = other.enabled;
            }
            if (id == null) {
                id = other.id;
            }
        }
    }

    @Override
    public ExternalResource clone() throws CloneNotSupportedException {
        ExternalResource other = (ExternalResource)super.clone();
        if (reserved != null) {
            other.reserved = reserved.clone();
        }
        if (locked != null) {
            other.locked = this.locked.clone();
        }
        return other;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put(JSON_ATTR_ID, id);
        json.put(JSON_ATTR_ENABLED, isEnabled());
        if (reserved != null) {
            json.put(JSON_ATTR_RESERVED, reserved.toJson());
        } else {
            json.put(JSON_ATTR_RESERVED, new JSONObject(true));
        }
        if (locked != null) {
            json.put(JSON_ATTR_LOCKED, locked.toJson());
        } else {
            json.put(JSON_ATTR_LOCKED, new JSONObject(true));
        }
        return json;
    }

    /**
     * Descriptor for {@link ExternalResource} metadata type.
     */
    @Extension
    public static final class ExternalResourceDescriptor extends TreeNodeMetaDataValueDescriptor {
        @Override
        public String getJsonType() {
            return Constants.SERIALIZATION_ALIAS_EXTERNAL_RESOURCE;
        }

        /**
         * Convenience method for easier reach via Jelly.
         *
         * @return the Disable/Enable permission.
         */
        @SuppressWarnings("unused")
        public Permission getDisablePermission() {
            return PluginImpl.ENABLE_DISABLE_EXTERNAL_RESOURCE;
        }

        @Override
        public MetadataValue fromJson(JSONObject json, MetadataContainer<MetadataValue> container)
                throws JsonUtils.ParseException {
            checkRequiredJsonAttribute(json, JSON_ATTR_ID);
            checkRequiredJsonAttribute(json, NAME);
            List<MetadataValue> children = new LinkedList<MetadataValue>();
            if (json.has(CHILDREN)) {
                JSONArray array = json.getJSONArray(CHILDREN);
                for (int i = 0; i < array.size(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    children.add(JsonUtils.toValue(obj, container));
                }
            }
            ExternalResource value = new ExternalResource(
                    json.getString(NAME), json.optString(DESCRIPTION),
                    json.getString(JSON_ATTR_ID), children);
            if (json.has(JSON_ATTR_ENABLED)) {
                container.getACL().checkPermission(PluginImpl.ENABLE_DISABLE_EXTERNAL_RESOURCE);
                value.setEnabled(json.getBoolean(JSON_ATTR_ENABLED));
            }
            if (json.has(EXPOSED)) {
                value.setExposeToEnvironment(json.getBoolean(EXPOSED));
            }
            if (json.has(GENERATED)) {
                value.setGenerated(json.getBoolean(GENERATED));
            } else {
                //TODO Decide if this is really what should be done.
                value.setGenerated(true);
            }
            return value;

        }

        @Override
        public boolean appliesTo(Descriptor containerDescriptor) {
            return containerDescriptor instanceof ExternalResourceTreeNode.ExternalResourceTreeNodeDescriptor;
        }

        @Override
        public List<AbstractMetaDataValueDescriptor> getValueDescriptors(StaplerRequest request) {
            Object containerObj = request.getAttribute(REQUEST_ATTR_METADATA_CONTAINER);
            request.setAttribute(REQUEST_ATTR_METADATA_CONTAINER, this);
            Descriptor container = null;
            if ((containerObj != null) && containerObj instanceof Descriptor) {
                container = (Descriptor)containerObj;
            }
            List<AbstractMetaDataValueDescriptor> list = new LinkedList<AbstractMetaDataValueDescriptor>();
            ExtensionList<AbstractMetaDataValueDescriptor> extensionList =
                    Hudson.getInstance().getExtensionList(AbstractMetaDataValueDescriptor.class);
            for (AbstractMetaDataValueDescriptor d : extensionList) {
                if (!(d instanceof ExternalResourceDescriptor) && d.appliesTo(container) && d.appliesTo(this)) {
                    list.add(d);
                }
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return Messages.ExternalResource_DisplayName();
        }
    }
}
