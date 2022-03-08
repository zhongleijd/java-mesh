/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.register.interceptors.health;

import com.huawei.register.context.RegisterContext;
import com.huawei.register.handler.SingleStateCloseHandler;
import com.huawei.sermant.core.common.LoggerFactory;
import com.huawei.sermant.core.plugin.agent.entity.ExecuteContext;

import org.springframework.cloud.consul.discovery.ConsulCatalogWatch;

import java.util.logging.Logger;

/**
 * 注册中心健康状态变更
 *
 * @author zhouss
 * @since 2021-12-13
 */
public class ConsulHealthInterceptor extends SingleStateCloseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    protected void close() {
        // 关闭consul心跳发送
        final Object registerWatch = RegisterContext.INSTANCE.getRegisterWatch();
        if (registerWatch instanceof ConsulCatalogWatch) {
            ConsulCatalogWatch watch = (ConsulCatalogWatch) registerWatch;
            watch.stop();
            LOGGER.info("Consul heartbeat has been closed.");
        }
    }

    @Override
    public ExecuteContext doAfter(ExecuteContext context) {
        final Object result = context.getResult();
        if (result != null) {
            // 原始注册中心恢复
            if (RegisterContext.INSTANCE.compareAndSet(false, true)) {
                doChange(context.getObject(), arguments, false, true);
            }
        }
        return context;
    }

    @Override
    public ExecuteContext doThrow(ExecuteContext context) {
        final boolean isOriginState = RegisterContext.INSTANCE.isAvailable();

        // 如果心跳为0L，则当前实例与consul注册中心不通，针对该实例注册中心已失效
        if (RegisterContext.INSTANCE.compareAndSet(true, false)) {
            doChange(context.getObject(), arguments, isOriginState, false);
        }
        return context;
    }
}
