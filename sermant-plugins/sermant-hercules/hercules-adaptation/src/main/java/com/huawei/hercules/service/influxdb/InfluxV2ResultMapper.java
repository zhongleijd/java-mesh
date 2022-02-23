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

package com.huawei.hercules.service.influxdb;

import com.influxdb.annotations.Column;
import com.influxdb.query.FluxRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 功能描述：把influxdb 2.x版本的record记录转化成对应的
 *
 * @author zl
 * @since 2022-02-23
 */
public class InfluxV2ResultMapper {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxV2ResultMapper.class);

    /**
     * 把记录中的值填充到指定的pojo中
     *
     * @param record 记录值
     * @param pojo   实体实力
     * @param fields 实例对应的属性列表
     * @param <T>    实体泛型
     */
    public <T> void fillPojo(FluxRecord record, T pojo, List<Field> fields) {
        if (record == null) {
            LOGGER.error("Record is required");
            return;
        }
        if (pojo == null) {
            LOGGER.error("Instance is required");
            return;
        }
        try {
            Map<String, Object> recordValues = record.getValues();
            for (Field field : fields) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                String columnName = field.getName();
                if (recordValues.containsKey(columnName)) {
                    setFieldValue(field, pojo, recordValues.get(columnName));
                    continue;
                }
                if (columnAnnotation == null) {
                    continue;
                }
                String columnAnnotationName = columnAnnotation.name();
                if (StringUtils.isEmpty(columnAnnotationName)) {
                    continue;
                }
                if (recordValues.containsKey(columnAnnotationName)) {
                    setFieldValue(field, pojo, recordValues.get(columnAnnotationName));
                    continue;
                }
                Object influxdbFieldName = recordValues.get("_field");
                if (columnName.equals(influxdbFieldName)
                        || columnAnnotationName.equals(influxdbFieldName)) {
                    setFieldValue(field, pojo, recordValues.get("_value"));
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * 设置数据实例对应的字段值
     *
     * @param field      字段
     * @param instance   字段所属的实例
     * @param filedValue 字段值
     * @throws IllegalAccessException 反射调用异常
     */
    private void setFieldValue(Field field, Object instance, Object filedValue) throws IllegalAccessException {
        if (field == null || instance == null) {
            LOGGER.error("Set entity instance field error, field={}, value={}", field, filedValue);
            return;
        }
        field.setAccessible(true);
        field.set(instance, filedValue);
    }
}
