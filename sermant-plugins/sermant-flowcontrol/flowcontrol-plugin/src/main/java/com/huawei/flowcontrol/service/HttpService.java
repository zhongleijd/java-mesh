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

package com.huawei.flowcontrol.service;

import com.huawei.flowcontrol.common.entity.FixedResult;
import com.huawei.flowcontrol.common.entity.RequestEntity;

/**
 * http拦截
 *
 * @author zhouss
 * @since 2022-01-25
 */
public interface HttpService {
    /**
     * 前置拦截
     *
     * @param requestEntity 请求信息
     * @param fixedResult 修正结果
     */
    void onBefore(RequestEntity requestEntity, FixedResult fixedResult);

    /**
     * 后置方法
     *
     * @param result 响应结果
     */
    void onAfter(Object result);

    /**
     * 异常抛出方法
     *
     * @param throwable 异常信息
     */
    void onThrow(Throwable throwable);
}
