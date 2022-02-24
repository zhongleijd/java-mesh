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

package com.huawei.argus.service.influxdb.query;

import com.huawei.argus.restcontroller.monitor.dto.MonitorHostDTO;
import com.huawei.argus.service.influxdb.metric.tree.MetricType;

import java.util.List;

/**
 * 功能描述：指标查询服务
 *
 * @since 2021-11-18
 */
public interface IMetricQueryService {
    /**
     * 获取指标类型
     *
     * @return 指标类型
     */
    MetricType getMetricType();

    /**
     * 获取指标数据
     *
     * @param monitorHostDTO 查询参数
     * @return 指定类型的数据列表
     */
    List<?> getMetricData(MonitorHostDTO monitorHostDTO);
}
