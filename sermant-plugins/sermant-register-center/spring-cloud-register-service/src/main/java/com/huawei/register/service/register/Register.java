/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.register.service.register;

import com.huawei.register.entity.MicroServiceInstance;

import java.util.List;

/**
 * 注册
 *
 * @author zhouss
 * @since 2021-12-17
 */
public interface Register {
    /**
     * 注册初始化
     */
    void start();

    /**
     * 停止方法
     */
    void stop();

    /**
     * 拦截原spring的注册方法
     */
    void register();

    /**
     * 替换服务列表 基于DiscoveryClient拦截
     *
     * @param <T> 实例信息
     * @param serviceId 服务ID
     * @return 服务列表
     */
    <T extends MicroServiceInstance> List<T> getInstanceList(String serviceId);

    /**
     * 注册中心类型
     *
     * @return register type
     */
    RegisterType registerType();

    enum RegisterType {
        /**
         * sc注册中心
         */
        SERVICE_COMB,

        /**
         * zk注册中心
         */
        ZOOKEEPER,

        /**
         * nacos注册中心
         */
        NACOS
    }
}
