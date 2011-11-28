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
 *  Pojo for the lease/expiration info.
 *  @author Leimeng Zhang
 */
public class Lease implements Serializable {

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
