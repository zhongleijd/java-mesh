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

package com.huawei.flowcontrol.core.config;

import java.nio.charset.Charset;

/**
 * 普通常量类
 *
 * @author liyi
 * @since 2020-08-26
 */
public class CommonConst {
    /**
     * metric消息发kafka时间间隔
     */
    public static final String METRIC_INTERVAL_MS = "sentinel.metric.interval.ms";

    /**
     * 周期性执行线程池名称
     */
    public static final String SENTINEL_SEND_CFC_TASK = "sentinel-send-cfc-task";

    /**
     * 线程池启动 延迟第一次执行的时间
     */
    public static final int INITIAL_DELAY = 5000;

    /**
     * 是否备份规则到redis，默认备份 ，控制台参数传费true是不备份
     */
    public static final String REDIS_RULE_STORE = "redis.rule.store";

    /**
     * 重试次数
     */
    public static final int RETRY_TIMES = 3;

    /**
     * 重试间隔
     */
    public static final int SLEEP_TIME = 1000;

    /**
     * 流控规则
     */
    public static final String SENTINEL_RULE_FLOW = "flow";

    /**
     * 降级规则
     */
    public static final String SENTINEL_RULE_DEGRADE = "degrade";

    /**
     * 授权规则
     */
    public static final String SENTINEL_RULE_AUTHORITY = "authority";

    /**
     * 系统规则
     */
    public static final String SENTINEL_RULE_SYSTEM = "system";

    /**
     * 客户端默认端口
     */
    public static final int SENTINEL_PORT = 8719;

    /**
     * redis配置规则数据源用到
     */
    public static final String CHANNEL = "channel";

    /**
     * sentinel
     */
    public static final String SENTINEL = "sentinel";

    /**
     * 冒号符号
     */
    public static final String COLON_SIGN = ":";

    /**
     * 斜杠符号
     */
    public static final String SLASH_SIGN = "/";

    /**
     * 等于符号
     */
    public static final String EQUAL_SIGN = "=";

    /**
     * 编码
     * 默认使用UTF-8
     */
    public static final Charset PLUGIN_ENCODE = Charset.forName("UTF-8");

    /**
     * 换行符号
     */
    public static final String NEWLINE_SIGN = System.getProperty("line.separator");

    /**
     * 逗号符号
     */
    public static final String COMMA_SIGN = ",";

    /**
     * sentinel配置参数 查询流控数据时,睡眠一段时间，等待限流数据写入文件再查询
     */
    public static final long METRIC_SLEEP_TIME_MILLIS = 1000L;

    /**
     * sentinel配置参数 心跳发送默认间隔时间，单位毫秒
     */
    public static final long FLOW_CONTROL_HEARTBEAT_INTERVAL = 10000L;

    /**
     * sentinel配置参数 流控信息数据发送默认间隔时间，单位毫秒
     */
    public static final long FLOW_CONTROL_METRIC_INTERVAL = 1000L;

    /**
     * sentinel配置参数 启动后初始加载流控信息数据的时间段时长
     */
    public static final long METRIC_INITIAL_DURATION = 60000L;

    /**
     * sentinel配置参数 未提供查询流控信息数据结束时间的默认加载数据条数
     */
    public static final long METRIC_MAX_LINE = 12000L;

    /**
     * sentinel配置参数 查询流控数据时,睡眠一段时间，等待限流数据写入文件再查询
     */
    public static final long METRIC_SLEEP_TIME = 2000L;

    /**
     * kafka配置参数 控制生产者发送请求最大大小,默认1M （这个参数和Kafka主机的message.max.bytes 参数有关系）
     */
    public static final long KAFKA_MAX_REQUEST_SIZE = 1048576L;

    /**
     * kafka配置参数 生产者内存缓冲区大小 32M
     */
    public static final long KAFKA_BUFFER_MEMORY = 33554432L;

    /**
     * kafka配置参数 客户端将等待请求的响应的最大时间
     */
    public static final long KAFKA_REQUEST_TIMEOUT_MS = 10000L;

    /**
     * kafka配置参数 最大阻塞时间，超过则抛出异常
     */
    public static final long KAFKA_MAX_BLOCK_MS = 60000L;

    /**
     * 配置中心zookeeper
     */
    public static final String CONFIG_TYPE_ZOOKEEPER = "zookeeper";

    /**
     * 配置中心kie
     */
    public static final String CONFIG_TYPE_KIE = "servicecomb-kie";

    /**
     * 配置中心kie，并适配cse
     */
    public static final String CONFIG_TYPE_CSE_KIE = "servicecomb-kie-cse-adapter";

    /**
     * 默认zookeeper地址
     */
    public static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private CommonConst() {
    }
}
