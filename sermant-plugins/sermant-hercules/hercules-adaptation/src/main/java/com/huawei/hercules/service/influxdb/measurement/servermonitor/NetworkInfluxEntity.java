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
 * server monitor network Influxdb持久化实体
 */
@Measurement(name = "server_monitor_network")
public class NetworkInfluxEntity extends CommonMetricInfluxEntity {
    /**
     * 采集周期内的每秒读字节数
     */
    @Column(name = "read_bytes_per_second")
    private Long readBytesPerSec;

    /**
     * 采集周期内的每秒写字节数
     */
    @Column(name = "write_bytes_per_second")
    private Long writeBytesPerSec;

    /**
     * 采集周期内的每秒读包数
     */
    @Column(name = "read_packages_per_second")
    private Long readPackagesPerSec;

    /**
     * 采集周期内的每秒写包数
     */
    @Column(name = "write_packages_per_second")
    private Long writePackagesPerSec;

    public Long getReadBytesPerSec() {
        return readBytesPerSec;
    }

    public void setReadBytesPerSec(Long readBytesPerSec) {
        this.readBytesPerSec = readBytesPerSec;
    }

    public Long getWriteBytesPerSec() {
        return writeBytesPerSec;
    }

    public void setWriteBytesPerSec(Long writeBytesPerSec) {
        this.writeBytesPerSec = writeBytesPerSec;
    }

    public Long getReadPackagesPerSec() {
        return readPackagesPerSec;
    }

    public void setReadPackagesPerSec(Long readPackagesPerSec) {
        this.readPackagesPerSec = readPackagesPerSec;
    }

    public Long getWritePackagesPerSec() {
        return writePackagesPerSec;
    }

    public void setWritePackagesPerSec(Long writePackagesPerSec) {
        this.writePackagesPerSec = writePackagesPerSec;
    }
}
