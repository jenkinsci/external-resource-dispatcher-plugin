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
import com.sonyericsson.hudson.plugins.metadata.model.MetadataParent;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import hudson.model.Node;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility for singling out available resources on a node.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public final class AvailabilityFilter {
    private static AvailabilityFilter ourInstance = new AvailabilityFilter();

    /**
     * This singleton instance.
     *
     * @return the instance.
     */
    public static AvailabilityFilter getInstance() {
        return ourInstance;
    }

    /**
     * Default constructor.
     */
    private AvailabilityFilter() {
    }

    /**
     * Finds the external resource attached to the node with the given id.
     *
     * @param node the node to search on.
     * @param id   the id of the resource to find.
     * @return the external resource if any.
     */
    public ExternalResource getExternalResourceById(Node node, String id) {
        MetadataNodeProperty property = node.getNodeProperties().get(MetadataNodeProperty.class);
        if (property != null) {
            return getExternalResourceById(property, id);
        } else {
            return null;
        }
    }

    /**
     * Finds the external resource below the parent with the given id.
     *
     * @param parent the parent to search in.
     * @param id     the id of the resource to find.
     * @return the external resource if any.
     */
    public ExternalResource getExternalResourceById(MetadataParent<MetadataValue> parent, String id) {
        if (parent instanceof ExternalResource) {
            ExternalResource resource = (ExternalResource)parent;
            if (resource.getId().equals(id)) {
                return resource;
            }
        } else {
            for (MetadataValue value : parent.getChildren()) {
                if (value instanceof ExternalResource) {
                    ExternalResource resource = (ExternalResource)value;
                    if (resource.getId().equals(id)) {
                        return resource;
                    }
                } else if (value instanceof MetadataParent) {
                    ExternalResource resource = getExternalResourceById((MetadataParent<MetadataValue>)value, id);
                    if (resource != null) {
                        return resource;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets all configured external resources on the node in a flat list.
     *
     * @param node the node to get them from.
     * @return a list of {@link ExternalResource}s or null if there is no metadata on the node.
     */
    public List<ExternalResource> getExternalResourcesList(Node node) {
        MetadataNodeProperty property = node.getNodeProperties().get(MetadataNodeProperty.class);
        if (property != null) {
            List<ExternalResource> list = new LinkedList<ExternalResource>();
            for (MetadataValue value : property.getChildren()) {
                populateExternalResourcesFrom(value, list);
            }
            return list;
        } else {
            return null;
        }
    }

    /**
     * Recursively searches the metadata value for any external resources and adds them to the provided list.
     *
     * @param value the value to "scan"
     * @param list  the list to populate.
     * @see #getExternalResourcesList(hudson.model.Node)
     */
    private void populateExternalResourcesFrom(MetadataValue value, List<ExternalResource> list) {
        if (value instanceof ExternalResource) {
            list.add((ExternalResource)value);
        } else if (value instanceof MetadataParent) {
            MetadataParent<MetadataValue> parent = (MetadataParent<MetadataValue>)value;
            for (MetadataValue child : parent.getChildren()) {
                populateExternalResourcesFrom(child, list);
            }
        }
    }
}
