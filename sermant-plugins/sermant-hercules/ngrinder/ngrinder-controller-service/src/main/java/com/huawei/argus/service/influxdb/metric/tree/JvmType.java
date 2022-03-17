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

package com.huawei.argus.service.influxdb.metric.tree;

/**
 * 功能描述：jvm类型
 *
 *
 * @since 2021-11-23
 */
public enum JvmType {
    /**
     * 非jvm类型
     */
    NONE("none"),

    /**
     * IBM版本jvm
     */
    IBM("ibm"),

    /**
     * open_jdk版本类型jvm
     */
    OPEN_JDK("open_jdk");

    /**
     * jvm类型标识符
     */
    private final String name;

    JvmType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
