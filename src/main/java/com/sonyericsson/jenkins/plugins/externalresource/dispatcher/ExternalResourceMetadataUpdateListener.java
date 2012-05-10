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
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher;


import com.sonyericsson.hudson.plugins.metadata.MetadataUpdateListener;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataParent;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResourceTreeNode;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.resourcemanagers.ExternalResourceManager;
import hudson.Extension;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A MetadataUpdateListener for ExternalResources.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@Extension
public class ExternalResourceMetadataUpdateListener extends MetadataUpdateListener {


    @Override
    public void metadataNodePropertyChanged(MetadataNodeProperty property) {
        List<ExternalResourceTreeNode> forest = findExternalResourceTreeNodes(property);
        for (ExternalResourceTreeNode tree : forest) {
            ExternalResourceManager manager = tree.findManager();
            manager.updateMetadata(tree);
        }
    }

    /**
     * Finds all the ExternalResourceTreeNodes in the MetadataParent.
     * @param property the MetadataParent to look for treenodes in.
     * @return a list of ExternalResourceTreeNodes.
     */
    private List<ExternalResourceTreeNode> findExternalResourceTreeNodes(MetadataParent<MetadataValue> property) {
        List<ExternalResourceTreeNode> treeNodes = new LinkedList<ExternalResourceTreeNode>();
        if (property != null) {
            Collection<MetadataValue> children = property.getChildren();
            for (MetadataValue child : children) {
                if (child instanceof ExternalResourceTreeNode) {
                    treeNodes.add((ExternalResourceTreeNode)child);
                } else if (child instanceof MetadataParent) {
                    treeNodes.addAll(findExternalResourceTreeNodes((MetadataParent<MetadataValue>)child));
                }
            }
        }
        return treeNodes;
    }
}
