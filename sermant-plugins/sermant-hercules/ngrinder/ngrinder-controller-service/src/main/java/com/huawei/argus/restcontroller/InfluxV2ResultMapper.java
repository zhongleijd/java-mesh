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

package com.huawei.argus.restcontroller;

import com.influxdb.annotations.Column;
import com.influxdb.query.FluxRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
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
     * @param <T>    实体泛型
     */
    public <T> void fillPojo(FluxRecord record, T pojo) {
        if (record == null) {
            LOGGER.error("Record is required");
            return;
        }
        if (pojo == null) {
            LOGGER.error("Instance is required");
            return;
        }
        try {
            Field[] fields = pojo.getClass().getDeclaredFields();
            Map<String, Object> recordValues = record.getValues();
            for (Field field : fields) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                String columnName = field.getName();
                if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
                    columnName = columnAnnotation.name();
                }
                String col;
                if (recordValues.containsKey(columnName)) {
                    col = columnName;
                } else if (recordValues.containsKey("_" + columnName)) {
                    col = "_" + columnName;
                } else if (recordValues.get("_field").equals(columnName)) {
                    col = "_value";
                } else {
                    LOGGER.error("The {} can not match any value in record[{}].", columnName, record);
                    continue;
                }
                Object value = record.getValueByKey(col);
                field.set(pojo, value);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
