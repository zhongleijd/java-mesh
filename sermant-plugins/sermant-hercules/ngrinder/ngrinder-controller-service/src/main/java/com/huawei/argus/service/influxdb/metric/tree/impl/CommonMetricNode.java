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

package com.huawei.argus.service.influxdb.metric.tree.impl;

import com.huawei.argus.restcontroller.monitor.dto.MonitorHostDTO;
import com.huawei.argus.service.influxdb.metric.tree.IMetricNode;
import com.huawei.argus.service.influxdb.metric.tree.JvmType;
import com.huawei.argus.service.influxdb.metric.tree.MetricType;
import com.huawei.argus.service.influxdb.query.IMetricQueryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 功能描述：指标类型基类
 *
 *
 * @since 2021-11-19
 */
public class CommonMetricNode implements IMetricNode {
    /**
     * 当前指标类型
     */
    private final MetricType currentMetricType;

    /**
     * 父指标类型
     */
    private final MetricType parentMetricType;

    /**
     * 子指标列表
     */
    private final List<IMetricNode> childMetrics = new ArrayList<>();

    /**
     * 数据查询服务
     */
    private final IMetricQueryService metricQueryService;

    /**
     * 当前指标数据
     */
    private List<?> metricData;

    public CommonMetricNode(MetricType currentMetricType, MetricType parentMetricType, IMetricQueryService metricQueryService) {
        if (currentMetricType == null) {
            throw new RuntimeException("Metric type can not be empty.");
        }
        this.currentMetricType = currentMetricType;
        this.parentMetricType = parentMetricType;
        this.metricQueryService = metricQueryService;
    }

    @Override
    public MetricType getCurrent() {
        return currentMetricType;
    }

    @Override
    public MetricType getParent() {
        return parentMetricType;
    }

    @Override
    public List<IMetricNode> getChildMetrics() {
        return childMetrics;
    }

    @Override
    public List<?> getMetricData() {
        return metricData;
    }

    @Override
    public void initMetric(List<IMetricNode> allMetrics, MonitorHostDTO monitorHostDTO) {
        // 如果指标为数据节点，说明当前指标是一个叶子指标，需要去influxdb查询数据
        if (currentMetricType.isDataNode()) {
            // 初始化当前指标数据，查询之前先判断是否满足展示这个指标的条件
            if (!canDisplay(monitorHostDTO)) {
                this.metricData = Collections.emptyList();
                return;
            }

            this.metricData = metricQueryService.getMetricData(monitorHostDTO);
            return;
        }

        // 如果指标不为数据节点，直接初始化子节点列表
        for (IMetricNode metric : allMetrics) {
            if (currentMetricType.equals(metric.getParent())) {
                // 在指标列表中找到父子表是当前指标的指标，并添加到当前指标的下一级子指标列表中
                childMetrics.add(metric);

                // 添加之后初始化子指标，递归调用每一级子指标
                metric.initMetric(allMetrics, monitorHostDTO);
            }
        }
    }

    /**
     * 子类实现自己的数据是否需要查询和展示
     *
     * @param monitorHostDTO 判断是否展示的参数
     * @return true:查询和展示，false：不查询和展示
     */
    public boolean canDisplay(MonitorHostDTO monitorHostDTO) {
        if (monitorHostDTO == null) {
            return false;
        }
        if (currentMetricType.getJvmType() == JvmType.NONE) {
            return true;
        }
        if (!monitorHostDTO.getIsMonitorJvm()) {
            return false;
        }
        return currentMetricType.getJvmType().getName().equals(monitorHostDTO.getJvmType());
    }
}
