/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.argus.service.influxdb;

import com.huawei.argus.restcontroller.monitor.dto.MonitorHostDTO;
import com.huawei.argus.service.influxdb.metric.tree.MetricTreeBuilder;
import com.huawei.argus.service.influxdb.metric.tree.impl.RootMetricNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 功能描述：获取监控数据服务
 *
 *
 * @since 2021-11-14
 */
@Service
public class CommonMonitorServiceImpl implements IMonitorService {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonMonitorServiceImpl.class);

    @Autowired
    private MetricTreeBuilder metricTreeBuilder;

    /**
     * 获取监控数据
     *
     * @param monitorHostDTO 查询数据参数
     * @return 查询数据
     */
    @Override
    public RootMetricNode getAllMonitorData(MonitorHostDTO monitorHostDTO) {
        LOGGER.debug("Start init metric, param:{}", monitorHostDTO);
        return metricTreeBuilder.buildMetricTree(monitorHostDTO);
    }
}
