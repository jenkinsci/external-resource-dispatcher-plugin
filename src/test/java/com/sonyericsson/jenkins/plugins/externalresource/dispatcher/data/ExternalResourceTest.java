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

import com.sonyericsson.hudson.plugins.metadata.model.MetadataJobProperty;
import com.sonyericsson.hudson.plugins.metadata.model.MetadataNodeProperty;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//CS IGNORE MagicNumber FOR NEXT 200 LINES. REASON: Test data.

/**
 * Tests for {@link ExternalResource} and it's descriptor.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class ExternalResourceTest {

    //CS IGNORE LineLength FOR NEXT 4 LINES. REASON: JavaDoc

    /**
     * Tests {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource.ExternalResourceDescriptor#appliesTo(hudson.model.Descriptor)}
     * that it doesn't apply to a Job.
     */
    @Test
    public void testAppliesToJob() {
        ExternalResource.ExternalResourceDescriptor descriptor = new ExternalResource.ExternalResourceDescriptor();
        assertFalse(descriptor.appliesTo(new MetadataJobProperty.MetaDataJobPropertyDescriptor()));
    }

    //CS IGNORE LineLength FOR NEXT 4 LINES. REASON: JavaDoc

    /**
     * Tests {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource.ExternalResourceDescriptor#appliesTo(hudson.model.Descriptor)}
     * that it does apply to a node.
     */
    @Test
    public void testAppliesToBuild() {
        ExternalResource.ExternalResourceDescriptor descriptor = new ExternalResource.ExternalResourceDescriptor();
        assertTrue(descriptor.appliesTo(new MetadataNodeProperty.MetadataNodePropertyDescriptor()));
    }

    /**
     * Tests {@link StashInfo.Lease#createInstance(long, int, String)}.
     * When the slave timeZone is in Japan and the local server is here.
     */
    @Test
    public void testLeaseCreateInstanceJapan() {
        // Given a time of 10am in Japan, get the server time
        Calendar japanCal = new GregorianCalendar(TimeZone.getTimeZone("Japan"));

        japanCal.set(Calendar.HOUR_OF_DAY, 10);            // 0..23
        japanCal.set(Calendar.MINUTE, 0);
        japanCal.set(Calendar.SECOND, 0);

        int seconds = (int)TimeUnit.MILLISECONDS.toSeconds(japanCal.getTimeZone().getRawOffset());

        StashInfo.Lease lease = StashInfo.Lease.createInstance(japanCal.getTimeInMillis(), seconds, "Japan RuleZ");

        Calendar local = new GregorianCalendar();
        int localOffset = (int)TimeUnit.MILLISECONDS.toHours(local.getTimeZone().getRawOffset());
        int japanOffset = (int)TimeUnit.SECONDS.toHours(seconds);

        assertEquals(10 - japanOffset + localOffset, lease.getServerTime().get(Calendar.HOUR_OF_DAY));
    }

    /**
     * Tests {@link StashInfo.Lease#createInstance(long, int, String)}.
     * When the slave timeZone is PST and the local server is here.
     */
    @Test
    public void testLeaseCreateInstanceSf() {
        // Given a time of 9am in San Francisco, get the server time
        Calendar sfCal = new GregorianCalendar(TimeZone.getTimeZone("PST"));

        sfCal.set(Calendar.HOUR_OF_DAY, 9);            // 0..23
        sfCal.set(Calendar.MINUTE, 0);
        sfCal.set(Calendar.SECOND, 0);

        int seconds = (int)TimeUnit.MILLISECONDS.toSeconds(sfCal.getTimeZone().getRawOffset());

        StashInfo.Lease lease = StashInfo.Lease.createInstance(sfCal.getTimeInMillis(), seconds, "SF RuleZ");

        Calendar local = new GregorianCalendar();
        int localOffset = (int)TimeUnit.MILLISECONDS.toHours(local.getTimeZone().getRawOffset());
        int sfOffset = (int)TimeUnit.SECONDS.toHours(seconds);

        assertEquals(9 - sfOffset + localOffset, lease.getServerTime().get(Calendar.HOUR_OF_DAY));
    }
}
