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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data;

import com.sonyericsson.hudson.plugins.metadata.model.JsonUtils;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Constants;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.Messages;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import hudson.Extension;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * Metadata type representing an external resource attached to a Node.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
@XStreamAlias(Constants.SERIALIZATION_ALIAS_EXTERNAL_RESOURCE)
public class ExternalResource extends TreeNodeMetadataValue {

    private String id;
    private StashInfo reserved;
    private StashInfo locked;

    /**
     * Standard DataBound Constructor.
     *
     * @param name        the name to identify it amongst its siblings.
     * @param description description
     * @param id          The unique ID of the resource
     * @param children    associated metadata.
     * @see TreeNodeMetadataValue#TreeNodeMetadataValue(String, String, java.util.List)
     */
    @DataBoundConstructor
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
     * Descriptor for {@link ExternalResource} metadata type.
     */
    @Extension
    public static final class ExternalResourceDescriptor extends TreeNodeMetaDataValueDescriptor {
        @Override
        public String getJsonType() {
            return Constants.SERIALIZATION_ALIAS_EXTERNAL_RESOURCE;
        }

        @Override
        public MetadataValue fromJson(JSONObject json) throws JsonUtils.ParseException {
            //TODO Implement
            return super.fromJson(json);
        }

        @Override
        public String getDisplayName() {
            return Messages.ExternalResource_DisplayName();
        }
    }
}
