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

package com.huawei.flowcontrol.adapte.cse.resolver;

import com.huawei.flowcontrol.adapte.cse.rule.RetryRule;

/**
 * 隔熔断配置解析
 *
 * @author zhouss
 * @since 2021-11-16
 */
public class RetryResolver extends AbstractResolver<RetryRule> {
    /**
     * 重试配置 键
     */
    public static final String CONFIG_KEY = "servicecomb.retry";

    public RetryResolver() {
        super(CONFIG_KEY);
    }

    @Override
    protected Class<RetryRule> getRuleClass() {
        return RetryRule.class;
    }
}
