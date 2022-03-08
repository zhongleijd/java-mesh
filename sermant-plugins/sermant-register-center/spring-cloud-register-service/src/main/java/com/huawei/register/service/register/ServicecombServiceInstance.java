/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.register.service.register;

import com.huawei.register.entity.MicroServiceInstance;
import com.huawei.register.service.utils.CommonUtils;

import org.apache.servicecomb.service.center.client.model.MicroserviceInstance;

import java.util.Map;

/**
 * service comb服务信息
 *
 * @author zhouss
 * @since 2022-02-17
 */
public class ServicecombServiceInstance implements MicroServiceInstance {
    private final MicroserviceInstance microserviceInstance;

    public ServicecombServiceInstance(MicroserviceInstance instance) {
        this.microserviceInstance = instance;
    }

    @Override
    public String getServiceName() {
        return microserviceInstance.getServiceName();
    }

    @Override
    public String getHost() {
        return microserviceInstance.getHostName();
    }

    @Override
    public int getPort() {
        return CommonUtils.getPortByEndpoint(microserviceInstance.getEndpoints().get(0));
    }

    @Override
    public String getServiceId() {
        return microserviceInstance.getServiceId();
    }

    @Override
    public String getInstanceId() {
        return microserviceInstance.getInstanceId();
    }

    @Override
    public Map<String, String> getMeta() {
        return microserviceInstance.getProperties();
    }
}
