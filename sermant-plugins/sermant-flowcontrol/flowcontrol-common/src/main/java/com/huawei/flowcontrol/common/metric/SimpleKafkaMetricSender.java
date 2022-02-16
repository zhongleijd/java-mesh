/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.flowcontrol.common.metric;

import com.huawei.flowcontrol.common.config.ConfigConst;
import com.huawei.flowcontrol.common.util.KafkaProducerUtil;
import com.huawei.flowcontrol.common.util.PluginConfigUtil;

/**
 * 发送sentinel客户端流控数据消息
 *
 * @author zhouss
 * @since 2021-01-28
 */
public class SimpleKafkaMetricSender extends AbstractMetricSender {
    @Override
    public void sendMetric(Object data) {
        // 调用kafka发送消息
        KafkaProducerUtil.sendMessage(PluginConfigUtil.getValueByKey(ConfigConst.KAFKA_METRIC_TOPIC), data.toString());
    }
}
