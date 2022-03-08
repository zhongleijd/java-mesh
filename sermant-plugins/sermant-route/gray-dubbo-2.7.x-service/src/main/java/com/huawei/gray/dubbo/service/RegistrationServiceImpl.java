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

package com.huawei.gray.dubbo.service;

import com.huawei.gray.dubbo.cache.DubboCache;
import com.huawei.gray.dubbo.utils.FieldPrivilegedAction;
import com.huawei.route.common.gray.addr.AddrCache;
import com.huawei.route.common.gray.constants.GrayConstant;
import com.huawei.route.common.gray.label.LabelCache;
import com.huawei.route.common.gray.label.entity.CurrentTag;
import com.huawei.route.common.gray.label.entity.GrayConfiguration;

import org.apache.servicecomb.service.center.client.model.Microservice;
import org.apache.servicecomb.service.center.client.model.MicroserviceInstance;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.List;

/**
 * RegistrationInterceptor的service
 *
 * @author provenceee
 * @since 2021/11/24
 */
public class RegistrationServiceImpl implements RegistrationService {
    /**
     * 拦截点前执行
     *
     * @param obj 增强的类
     * @param arguments 增强方法的所有参数
     */
    @Override
    public void getVersion(Object obj, Object[] arguments) throws NoSuchFieldException, IllegalAccessException {
        if (arguments.length <= 2 || !(arguments[2] instanceof List<?>)) {
            return;
        }
        Field field = obj.getClass().getDeclaredField("microservice");
        AccessController.doPrivileged(new FieldPrivilegedAction(field));
        Microservice microservice = (Microservice) field.get(obj);
        GrayConfiguration grayConfiguration = LabelCache.getLabel(DubboCache.getLabelName());
        CurrentTag currentTag = grayConfiguration.getCurrentTag();
        currentTag.setRegisterVersion(microservice.getVersion());
        List<MicroserviceInstance> instances = (List<MicroserviceInstance>) arguments[2];
        for (MicroserviceInstance instance : instances) {
            for (String endpoint : instance.getEndpoints()) {
                AddrCache.setRegisterVersionCache(endpoint.substring(GrayConstant.DUBBO_PREFIX.length()),
                    instance.getVersion());
            }
        }
    }
}