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

package com.huawei.flowre.flowreplay.invocation;

import com.huawei.flowre.flowreplay.config.Const;
import com.huawei.flowre.flowreplay.domain.message.DubboInvokeMessage;
import com.huawei.flowre.flowreplay.domain.message.HttpInvokeMessage;
import com.huawei.flowre.flowreplay.domain.message.ReplayResultMessage;
import com.huawei.flowre.flowreplay.domain.result.DubboRequestResult;
import com.huawei.flowre.flowreplay.domain.result.HttpRequestResult;
import com.huawei.flowre.flowreplay.utils.RpsCalculateUtil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import lombok.AllArgsConstructor;
import lombok.Builder;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 回放线程逻辑
 *
 * @author luanwenfei
 * @version 0.0.1
 * @since 2021-06-28
 */
@AllArgsConstructor
@Builder
public class InvokeThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvokeThread.class);
    /**
     * 回放数据
     */
    ConsumerRecords<String, String> records;

    /**
     * rps计数器
     */
    RpsCalculateUtil rpsCalculateUtil;

    /**
     * kafka 生产者 向kafka发送回放结果
     */
    KafkaProducer<String, String> kafkaProducer;

    /**
     * 回放结果topic 根据主机ip和端口生成
     */
    String replayResultTopic;

    /**
     * http请求工具
     */
    HttpReplayInvocationImpl httpReplayInvocation;

    /**
     * dubbo请求工具
     */
    DubboReplayInvocationImpl dubboReplayInvocation;

    /**
     * 向kafka发送回放结果数据
     *
     * @param resultMessage 回放结果数据包括回放结果和录制时的接口响应结果
     */
    private void sendReplayResultMessage(ReplayResultMessage resultMessage) {
        try {
            kafkaProducer.send(new ProducerRecord<>(replayResultTopic, null,
                JSON.toJSONString(resultMessage, SerializerFeature.WriteMapNullValue)));
        } catch (Exception exception) {
            LOGGER.error("Send replay result message to kafka error : {}", exception.getMessage());
        } finally {
            kafkaProducer.flush();
        }
    }

    @Override
    public void run() {
        for (ConsumerRecord<String, String> record : records) {
            ReplayResultMessage replayResultMessage = null;
            switch (JSON.parseObject(record.value()).getString("type")) {
                case Const.DUBBO_TYPE: {
                    DubboInvokeMessage dubboInvokeMessage = JSON.parseObject(record.value(), DubboInvokeMessage.class);
                    DubboRequestResult dubboRequestResult =
                        dubboReplayInvocation.invoke(dubboInvokeMessage.getHttpInvokeContent());
                    replayResultMessage = ReplayResultMessage.builder().traceId(dubboInvokeMessage.getTraceId())
                        .replayJobId(dubboInvokeMessage.getReplayJobId())
                        .methodName(dubboInvokeMessage.getMethodName())
                        .responseBody(dubboInvokeMessage.getResponseBody())
                        .replayResult(JSON.toJSONString(dubboRequestResult.getResult()))
                        .replayTime(new Date())
                        .recordTime(dubboInvokeMessage.getRecordTime())
                        .statusCode(dubboRequestResult.getStatusCode())
                        .responseTime(dubboRequestResult.getResponseTime())
                        .build();
                    break;
                }
                case Const.HTTP_TYPE: {
                    HttpInvokeMessage httpInvokeMessage = JSON.parseObject(record.value(), HttpInvokeMessage.class);
                    HttpRequestResult httpRequestResult = httpReplayInvocation.invoke(httpInvokeMessage);
                    replayResultMessage = ReplayResultMessage.builder().traceId(httpInvokeMessage.getTraceId())
                        .replayJobId(httpInvokeMessage.getReplayJobId())
                        .methodName(httpInvokeMessage.getMethodName())
                        .responseBody(httpInvokeMessage.getResponseBody())
                        .replayResult(httpRequestResult.getResponseBody())
                        .recordTime(httpInvokeMessage.getRecordTime())
                        .replayTime(new Date())
                        .statusCode(httpRequestResult.getStatusCode())
                        .responseTime(httpRequestResult.getResponseTime())
                        .build();
                    break;
                }
                default:
            }
            rpsCalculateUtil.calculateRps();
            sendReplayResultMessage(replayResultMessage);
        }
    }
}
