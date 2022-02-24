/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.argus.service.influxdb.measurement.openjdk;

import com.huawei.argus.service.influxdb.measurement.CommonMetricInfluxEntity;
import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

/**
 * Oracle jvm metric thread Influxdb持久化实体
 */
@Measurement(name = "oracle_jvm_monitor_thread")
public class ThreadInfluxEntity extends CommonMetricInfluxEntity {

    @Column(name = "live_count")
    private double liveCount;

    @Column(name = "daemon_count")
    private double daemonCount;

    @Column(name = "peak_count")
    private double peakCount;

    public double getLiveCount() {
        return liveCount;
    }

    public void setLiveCount(double liveCount) {
        this.liveCount = liveCount;
    }

    public double getDaemonCount() {
        return daemonCount;
    }

    public void setDaemonCount(double daemonCount) {
        this.daemonCount = daemonCount;
    }

    public double getPeakCount() {
        return peakCount;
    }

    public void setPeakCount(double peakCount) {
        this.peakCount = peakCount;
    }
}
