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

package com.huawei.hercules.service.influxdb.measurement;

import com.influxdb.annotations.Column;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * 通用Influxdb持久化实体
 */
public abstract class CommonMetricInfluxEntity {
    @Column(timestamp = true, name = "_time")
    private Instant time;

    @Column(tag = true, name = "service")
    private String service;

    @Column(tag = true, name = "service_instance")
    private String serviceInstance;

    public String getTime() {
        return time.getEpochSecond() +""+ time.getNano();
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(String serviceInstance) {
        this.serviceInstance = serviceInstance;
    }
}
