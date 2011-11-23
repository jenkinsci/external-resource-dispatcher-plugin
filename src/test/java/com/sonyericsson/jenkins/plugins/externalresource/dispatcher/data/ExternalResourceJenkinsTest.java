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

import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import com.sonyericsson.hudson.plugins.metadata.model.values.AbstractMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.DateMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.StringMetadataValue;
import com.sonyericsson.hudson.plugins.metadata.model.values.TreeNodeMetadataValue;
import hudson.model.Hudson;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;

import static com.sonyericsson.hudson.plugins.metadata.Constants.REQUEST_ATTR_METADATA_CONTAINER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link HudsonTestCase}s for {@link ExternalResource}.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class ExternalResourceJenkinsTest extends HudsonTestCase {

    //CS IGNORE LineLength FOR NEXT 4 LINES. REASON: JavaDoc
    /**
     * Tests
     * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource.ExternalResourceDescriptor#getValueDescriptors(org.kohsuke.stapler.StaplerRequest)}.
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
}
