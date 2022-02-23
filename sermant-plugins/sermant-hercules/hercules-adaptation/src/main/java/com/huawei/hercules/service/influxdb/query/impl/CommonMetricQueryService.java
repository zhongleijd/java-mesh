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

package com.huawei.hercules.service.influxdb.query.impl;

import com.huawei.hercules.controller.monitor.dto.MonitorHostDTO;
import com.huawei.hercules.exception.HerculesException;
import com.huawei.hercules.service.influxdb.metric.tree.MetricType;
import com.huawei.hercules.service.influxdb.query.IMetricQueryService;
import com.huawei.hercules.service.influxdb.query.InfluxDBSqlExecutor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 功能描述：query metric base class
 *
 * @since 2021-11-19
 */
public class CommonMetricQueryService implements IMetricQueryService {
    /**
     * SQL模板
     */
    private final String baseSql = "from(bucket:\"%s\")" +
            "|> range(" +
            "   start : %s %s " +
            ")" +
            "|> filter( fn: (r) => " +
            "   r._measurement == \"%s\" and r.service == \"%s\" and r.service_instance == \"%s\"" +
            ")";

    /**
     * metric type
     */
    private final MetricType metricType;

    /**
     * sql of query executor
     */
    private final InfluxDBSqlExecutor influxDBSqlExecutor;

    /**
     * 数据实例类型
     */
    private Class<?> dataEntityClass;

    /**
     * init metric type and child metric
     *
     * @param metricType this metric type
     */
    public CommonMetricQueryService(MetricType metricType,
                                    InfluxDBSqlExecutor influxDBSqlExecutor,
                                    Class<?> dataEntityClass) {
        if (StringUtils.isEmpty(metricType)) {
            throw new HerculesException("Metric type can not be empty.");
        }
        if (influxDBSqlExecutor == null) {
            throw new HerculesException("Query executor is empty.");
        }
        this.influxDBSqlExecutor = influxDBSqlExecutor;
        this.metricType = metricType;
        this.dataEntityClass = dataEntityClass;
    }

    @Override
    public MetricType getMetricType() {
        return metricType;
    }

    @Override
    public List<?> getMetricData(MonitorHostDTO monitorHostDTO) {
        return influxDBSqlExecutor.execute(buildSql(monitorHostDTO), dataEntityClass);
    }

    /**
     * build metric query data
     *
     * @param monitorHostDTO query param
     * @return sql
     */
    public String buildSql(MonitorHostDTO monitorHostDTO) {
        if (monitorHostDTO == null) {
            throw new HerculesException("The data for query is null.");
        }
        if (StringUtils.isEmpty(monitorHostDTO.getBucket())) {
            throw new HerculesException("Bucket in influxdb can not be null.");
        }
        return String.format(
                Locale.ENGLISH,
                baseSql,
                monitorHostDTO.getBucket(),
                monitorHostDTO.getStartTime(),
                StringUtils.isEmpty(monitorHostDTO.getEndTime()) ? "" : ", stop : " + monitorHostDTO.getEndTime(),
                metricType.getName(),
                monitorHostDTO.getService(),
                monitorHostDTO.getServiceInstance());
    }
}
