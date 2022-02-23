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

package com.huawei.hercules.service.influxdb.measurement.servermonitor;

import com.huawei.hercules.service.influxdb.measurement.CommonMetricInfluxEntity;
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
    private Long memoryTotal;

    /**
     * 已使用的内存大小
     */
    @Column(name = "swap_cached")
    private Long swapCached;

    /**
     * 对应cat /proc/meminfo指令的Cached
     */
    @Column(name ="cached")
    private Long cached;

    /**
     * 对应cat /proc/meminfo指令的Buffers
     */
    @Column(name ="buffers")
    private Long buffers;

    /**
     * 对应cat /proc/meminfo指令的SwapCached
     */
    @Column(name ="memory_used")
    private Long memoryUsed;

    public Long getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public Long getSwapCached() {
        return swapCached;
    }

    public void setSwapCached(Long swapCached) {
        this.swapCached = swapCached;
    }

    public Long getCached() {
        return cached;
    }

    public void setCached(Long cached) {
        this.cached = cached;
    }

    public Long getBuffers() {
        return buffers;
    }

    public void setBuffers(Long buffers) {
        this.buffers = buffers;
    }

    public Long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(Long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }
}
