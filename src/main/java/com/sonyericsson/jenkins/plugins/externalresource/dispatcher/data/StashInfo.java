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

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Information about the "stashed" status of an {@link ExternalResource}. I.e. The lock and reservation status.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class StashInfo implements Serializable {
    private String stashedBy;
    private StashType type;
    private Lease lease;
    private String key;

    /**
     * Standard Constructor.
     *
     * @param type      the general type of what stashed the resource.
     * @param stashedBy exactly what stashed it.
     * @param lease     when will it end.
     * @param key       the key to use when releasing.
     */
    public StashInfo(StashType type, String stashedBy, Lease lease, String key) {
        this.stashedBy = stashedBy;
        this.type = type;
        this.lease = lease;
        this.key = key;
    }

    /**
     * Creates a new object with info from the StashResult.
     * The type will be defaulted to {@link StashInfo.StashType#INTERNAL}.
     * @param result the result from a
     *      {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager} operation.
     * @param stashedBy the build that it belongs to.
     */
    public StashInfo(StashResult result, String stashedBy) {
        this.stashedBy = stashedBy;
        this.type = StashType.INTERNAL;
        this.key = result.getKey();
        //TODO when StashResult has the new design implementation use that info.
        this.lease = Lease.createInstance(result.getLeaseExpireTime(), 1, "");
    }

    /**
     * Exactly what stashed it. If {@link #getType()} is {@link StashType#INTERNAL} then this points to a build on the
     * local Jenkins server.
     *
     * @return the name or something else.
     */
    public String getStashedBy() {
        return stashedBy;
    }

    /**
     * General type info about what/who stashed the resource.
     *
     * @return the type.
     */
    public StashType getType() {
        return type;
    }

    /**
     * Information about when the lease expires.
     *
     * @return the lease info.
     */
    public Lease getLease() {
        return lease;
    }

    /**
     * The key to release or lock the resource.
     *
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * If {@link #getType()} is internal or not. Added for simplified usage in jelly.
     *
     * @return true if so.
     */
    public boolean isInternal() {
        return getType() == StashType.INTERNAL;
    }

    /**
     * Pojo for the lease/expiration info.
     */
    public static class Lease implements Serializable {
        /**
         * The threshold for a GMT hour or minute id before it needs to be prefixed with '0'. i.e. 10
         */
        protected static final int ZERO_PREFIX_THRESHOLD = 10;

        /**
         * The threshold for the GMT hour sign, below this the sign should be '-'. i.e. 0.
         */
        protected static final int NEGATIVE_THRESHOLD = 0;
        private Calendar serverTime;
        private String slaveIsoTime;

        /**
         * Standard constructor.
         *
         * @param serverTime   the time frame of the lease in server time.
         * @param slaveIsoTime The time according to the service in ISO 8601 format
         */
        public Lease(Calendar serverTime, String slaveIsoTime) {
            this.serverTime = serverTime;
            this.slaveIsoTime = slaveIsoTime;
        }

        /**
         * Factory fro creating a Lease from the DeviceMonitor service values.
         *
         * @param slaveTime     The time in milliseconds since the epoch, in UTC when the lease expires.
         * @param slaveTimeZone The offset of the local timezone, in seconds west of UTC.
         * @param slaveIsoTime  The same value as time but in ISO 8601 format.
         * @return A created Lease object.
         */
        public static Lease createInstance(long slaveTime, int slaveTimeZone, String slaveIsoTime) {
            Calendar calendar = Calendar.getInstance(createTimeZone(slaveTimeZone));
            calendar.setTimeInMillis(slaveTime);
            Calendar local = Calendar.getInstance();
            local.setTimeInMillis(calendar.getTimeInMillis());
            return new Lease(local, slaveIsoTime);
        }

        /**
         * Creates a TimeZone instance from the given timeOffset
         *
         * @param timeOffset the GMT offset in seconds.
         * @return the TimeZone.
         */
        private static TimeZone createTimeZone(int timeOffset) {
            String sign = "+";
            if (timeOffset < NEGATIVE_THRESHOLD) {
                sign = "-";
                timeOffset = timeOffset * -1;
            }
            StringBuilder id = new StringBuilder("GMT").append(sign);

            long hours = TimeUnit.SECONDS.toHours(timeOffset);
            long left = timeOffset - TimeUnit.HOURS.toSeconds(hours);
            long minutes = TimeUnit.SECONDS.toMinutes(left);
            if (hours < ZERO_PREFIX_THRESHOLD) {
                id.append('0');
            }
            id.append(hours);
            if (minutes > 0) {
                id.append(':');
                if (minutes < ZERO_PREFIX_THRESHOLD) {
                    id.append('0');
                }
                id.append(minutes);
            }
            return TimeZone.getTimeZone(id.toString());
        }

        /**
         * The time in the local server timezone when the lease expires.
         *
         * @return the time.
         */
        public Calendar getServerTime() {
            return serverTime;
        }

        /**
         * The time on the slave when the lease expires. In ISO 8601 format.
         *
         * @return the time.
         */
        public String getSlaveIsoTime() {
            return slaveIsoTime;
        }
    }

    /**
     * The general type of what stashed the resource.
     */
    public static enum StashType {
        /**
         * The resource is stashed by Jenkins, so stashedBy should point to a build.
         */
        INTERNAL,
        /**
         * The resource is stashed by something external, so stashed by points to something else.
         */
        EXTERNAL
    }
}
