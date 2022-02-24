/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.argus.service.influxdb.metric.tree.impl;

import com.huawei.argus.restcontroller.monitor.dto.MonitorHostDTO;
import com.huawei.argus.service.influxdb.metric.tree.IMetricNode;
import com.huawei.argus.service.influxdb.metric.tree.JvmType;
import com.huawei.argus.service.influxdb.metric.tree.MetricType;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能描述：数据根节点
 *
 * @author zl
 * @since 2022-02-24
 */
public class RootMetricNode {
    /**
     * 服务器指标
     */
    private IMetricNode serverMetric;

    /**
     * openJdk指标
     */
    private IMetricNode openJdkMetric;

    /**
     * ibm指标
     */
    private IMetricNode ibmJdkMetric;

    /**
     * 指标池
     */
    private List<IMetricNode> metrics;

    /**
     * 根节点初始化
     *
     * @param allMetrics     所有需要查询的指标
     * @param monitorHostDTO 查询条件
     */
    public void initMetric(List<IMetricNode> allMetrics, MonitorHostDTO monitorHostDTO) {
        for (IMetricNode oneMetric : allMetrics) {
            if (oneMetric.getCurrent().equals(MetricType.SERVER)) {
                this.serverMetric = oneMetric;
                continue;
            }
            if (oneMetric.getCurrent().equals(MetricType.OPEN_JDK)) {
                this.openJdkMetric = oneMetric;
                continue;
            }
            if (oneMetric.getCurrent().equals(MetricType.IBM)) {
                this.ibmJdkMetric = oneMetric;
            }
        }
        metrics = new ArrayList<>();
        serverMetric.initMetric(allMetrics, monitorHostDTO);
        metrics.add(serverMetric);
        if (!monitorHostDTO.getIsMonitorJvm()) {
            return;
        }
        if (monitorHostDTO.getJvmType().equals(JvmType.OPEN_JDK.getName())) {
            openJdkMetric.initMetric(allMetrics, monitorHostDTO);
            metrics.add(openJdkMetric);
        } else {
            ibmJdkMetric.initMetric(allMetrics, monitorHostDTO);
            metrics.add(ibmJdkMetric);
        }
    }

    public List<IMetricNode> getMetrics() {
        return metrics;
    }
}
