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

package com.huawei.argus.service.influxdb.measurement.servermonitor;

import com.huawei.argus.service.influxdb.measurement.CommonMetricInfluxEntity;
import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

/**
 * server monitor cpu Influxdb持久化实体
 */
@Measurement(name = "server_monitor_cpu")
public class CpuInfluxEntity extends CommonMetricInfluxEntity {
    /**
     * idle时间百分占比
     */
    @Column(name = "idle_percentage")
    private double idlePercentage;

    /**
     * io wait时间百分占比
     */
    @Column(name = "io_wait_percentage")
    private double ioWaitPercentage;

    /**
     * sys时间百分占比
     */
    @Column(name = "sys_percentage")
    private double sysPercentage;

    /**
     * user和nice时间百分占比
     */
    @Column(name = "user_percentage")
    private double userPercentage;

    public double getIdlePercentage() {
        return idlePercentage;
    }

    public void setIdlePercentage(double idlePercentage) {
        this.idlePercentage = idlePercentage;
    }

    public double getIoWaitPercentage() {
        return ioWaitPercentage;
    }

    public void setIoWaitPercentage(double ioWaitPercentage) {
        this.ioWaitPercentage = ioWaitPercentage;
    }

    public double getSysPercentage() {
        return sysPercentage;
    }

    public void setSysPercentage(double sysPercentage) {
        this.sysPercentage = sysPercentage;
    }

    public double getUserPercentage() {
        return userPercentage;
    }

    public void setUserPercentage(double userPercentage) {
        this.userPercentage = userPercentage;
    }
}
