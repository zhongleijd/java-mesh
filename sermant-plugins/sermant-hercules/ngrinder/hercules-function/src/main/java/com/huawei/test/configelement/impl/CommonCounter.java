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

package com.huawei.test.configelement.impl;

import com.huawei.test.configelement.Counter;
import com.huawei.test.configelement.config.CounterConfig;
import com.huawei.test.configelement.service.ExecuteTimesInfo;
import com.huawei.test.exception.FunctionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 功能描述：计数器实现
 *
 * @author zl
 * @since 2021-12-09
 */
public class CommonCounter extends Counter {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonCounter.class);

    /**
     * config配置
     */
    private CounterConfig counterConfig;

    /**
     * 缓存该线程能使用的数字
     */
    private final List<Integer> cacheNumber = new ArrayList<>();

    /**
     * 当前取数的游标
     */
    private int cursor = 0;

    /**
     * 需要返回的字符串格式
     */
    private String numberFormat;

    @Override
    public void initConfig(CounterConfig config) {
        this.counterConfig = config;
        if (!isConfigValid()) {
            return;
        }

        // 字符串格式
        numberFormat = config.getNumberFormat();
        if (numberFormat == null) {
            numberFormat = "%d";
        }

        // 通过模拟测试方法执行次数，提前算出该线程能获取的计数器数字放入缓存中
        int runNumber = 0;
        int nextNumber;
        while ((nextNumber = nextValue(runNumber)) <= config.getMaxValue()) {
            LOGGER.debug("Add number {} to cache.", nextNumber);
            cacheNumber.add(nextNumber);
            runNumber++;
        }
    }

    @Override
    public boolean isConfigValid() {
        if (counterConfig == null) {
            LOGGER.error("The counter config is null.");
            return false;
        }
        return counterConfig.getSharingMode() != null;
    }

    @Override
    public String nextNumber() {
        if (cacheNumber.isEmpty()) {
            LOGGER.error("No matched number for this thread calculating by config.");
            throw new FunctionException("No matched number for this thread calculating by config.");
        }
        if (cursor < cacheNumber.size()) {
            String formatValue = String.format(Locale.ENGLISH, numberFormat, cacheNumber.get(cursor++));
            LOGGER.debug("Return counter value:{}", formatValue);
            return formatValue;
        }
        if (counterConfig.resetEachIteration()) {
            // 走到这里说明当设置了重复取值的时候，重置游标，从第一个开始取值
            cursor = 0;
            String formatValue = String.format(Locale.ENGLISH, numberFormat, cacheNumber.get(cursor));
            LOGGER.debug("Start counter again, Return counter value:{}", formatValue);
            return formatValue;
        }
        Integer maxValue = counterConfig.getMaxValue();
        LOGGER.error("Over max value:{}", maxValue);
        throw new FunctionException("Over max value:" + maxValue);
    }

    @Override
    public boolean hasNext() {
        if (cursor < cacheNumber.size()) {
            return true;
        }
        return !cacheNumber.isEmpty() && counterConfig.resetEachIteration();
    }

    /**
     * 通过config配置计算下一个值
     *
     * @return 下一个值
     */
    protected int nextValue(int runNumber) {
        ExecuteTimesInfo executeTimesInfo = getExecuteTimesInfo(runNumber);
        int nextValue = counterConfig.getSharingMode().getGrinderCountService().nextIncrementNumber(executeTimesInfo);
        int startValue = counterConfig.getStartValue();
        int incrementRange = counterConfig.getIncrement();
        return Math.addExact(Math.multiplyExact(nextValue, incrementRange), startValue);
    }
}
