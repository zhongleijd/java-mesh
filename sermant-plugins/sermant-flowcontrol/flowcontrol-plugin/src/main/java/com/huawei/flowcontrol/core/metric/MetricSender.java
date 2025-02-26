/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.flowcontrol.core.metric;

import com.huawei.flowcontrol.core.metric.provider.MetricProvider;

/**
 * 指标发送器
 *
 * @author zhouss
 * @since 2021-12-07
 */
public interface MetricSender {
    /**
     * 发送指标数据
     *
     * @param data 发送数据
     */
    void sendMetric(Object data);

    /**
     * 默认指标数据发送
     */
    void sendMetric();

    /**
     * 基于提供provider发送数据
     *
     * @param metricProvider 指标provider
     */
    void sendMetric(MetricProvider metricProvider);
}
