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

package com.huawei.register.declarers;

import com.huawei.register.interceptors.ClientConfigInterceptor;
import com.huawei.sermant.core.plugin.agent.declarer.AbstractPluginDeclarer;
import com.huawei.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import com.huawei.sermant.core.plugin.agent.matcher.ClassMatcher;
import com.huawei.sermant.core.plugin.agent.matcher.MethodMatcher;

/**
 * 针对eureka，consul注册中心获取实例列表拦截
 *
 * @author zhouss
 * @since 2021-12-17
 */
public class ClientConfigDeclarer extends AbstractPluginDeclarer {
    /**
     * 增强类的全限定名 该client注入优先级最高，因此只需拦截该client即可
     */
    private static final String ENHANCE_CLASS = "com.netflix.client.config.IClientConfig";

    /**
     * 拦截类的全限定名
     */
    private static final String INTERCEPT_CLASS = ClientConfigInterceptor.class.getCanonicalName();

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.isExtendedFrom(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
            InterceptDeclarer.build(MethodMatcher.isConstructor(), INTERCEPT_CLASS)
        };
    }
}
