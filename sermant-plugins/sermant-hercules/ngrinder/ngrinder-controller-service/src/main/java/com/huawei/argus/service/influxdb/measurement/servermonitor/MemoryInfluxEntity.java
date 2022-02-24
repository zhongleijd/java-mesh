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
 * server monitor memory Influxdb持久化实体
 */
@Measurement(name = "server_monitor_memory")
public class MemoryInfluxEntity extends CommonMetricInfluxEntity {
    /**
     * 总内存大小
     */
    @Column(name = "memory_total")
    private double memoryTotal;

    /**
     * 已使用的内存大小
     */
    @Column(name = "swap_cached")
    private double swapCached;

    /**
     * 对应cat /proc/meminfo指令的Cached
     */
    @Column(name ="cached")
    private double cached;

    /**
     * 对应cat /proc/meminfo指令的Buffers
     */
    @Column(name ="buffers")
    private double buffers;

    /**
     * 对应cat /proc/meminfo指令的SwapCached
     */
    @Column(name ="memory_used")
    private double memoryUsed;

    public double getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(double memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public double getSwapCached() {
        return swapCached;
    }

    public void setSwapCached(double swapCached) {
        this.swapCached = swapCached;
    }

    public double getCached() {
        return cached;
    }

    public void setCached(double cached) {
        this.cached = cached;
    }

    public double getBuffers() {
        return buffers;
    }

    public void setBuffers(double buffers) {
        this.buffers = buffers;
    }

    public double getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(double memoryUsed) {
        this.memoryUsed = memoryUsed;
    }
}
