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

import com.huawei.argus.config.InfluxDBConfig;
import com.huawei.argus.service.influxdb.InfluxV2ResultMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 功能描述：执行sql，返回执行的类型列表数据
 *
 * @since 2021-11-14
 */
@Service
public class InfluxDBSqlExecutor {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDBSqlExecutor.class);

    /**
     * influxdb client
     */
    @Autowired
    private InfluxDBClient influxDBClient;

    @Autowired
    private InfluxDBConfig influxDBConfig;

    /**
     * query api for influxdb
     */
    private QueryApi queryApi;

    @PostConstruct
    public void init() {
        this.queryApi = influxDBClient.getQueryApi();
    }

    /**
     * 执行sql，返回泛型指定类型的列表数据
     *
     * @param conditionSql sql语句
     * @param clazz        数据类型
     * @param <T>          数据类型
     * @return 列表集合
     */
    public <T> List<T> execute(String conditionSql, Class<T> clazz) {
        if (StringUtils.isEmpty(conditionSql) || clazz == null) {
            return Collections.emptyList();
        }
        List<FluxTable> tables = queryApi.query(conditionSql, influxDBConfig.getOrg());
        if (tables.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> recordsList = new ArrayList<>();
        InfluxV2ResultMapper influxV2ResultMapper = new InfluxV2ResultMapper();
        int dataCount = tables.get(0).getRecords().size();
        for (int i = 0; i < dataCount; i++) {
            try {
                T entityInstance = clazz.newInstance();
                for (FluxTable fluxTable : tables) {
                    FluxRecord oneRecord = fluxTable.getRecords().get(i);
                    influxV2ResultMapper.fillPojo(oneRecord, entityInstance, getEntityField(clazz));
                }
                recordsList.add(entityInstance);
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.error("Create entity instance fail.", e);
            }
        }
        return recordsList;
    }

    /**
     * 获取一个类型所有定义的属性名称
     *
     * @param clazz 类型
     * @return 类型所有的属性名称
     */
    private List<Field> getEntityField(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        fields.addAll(getEntityField(clazz.getSuperclass()));
        return fields;
    }
}
