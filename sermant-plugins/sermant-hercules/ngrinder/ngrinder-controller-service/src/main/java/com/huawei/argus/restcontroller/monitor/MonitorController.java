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

package com.huawei.argus.restcontroller.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huawei.argus.restcontroller.monitor.dto.MonitorHostDTO;
import com.huawei.argus.service.influxdb.IMonitorService;
import com.huawei.argus.service.influxdb.metric.tree.impl.RootMetricNode;
import org.ngrinder.service.IPerfTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能描述：监控分析数据获取
 *
 * @since 2021-11-12
 */
@RestController
@RequestMapping("/rest/monitor")
public class MonitorController {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private IMonitorService monitorService;

    @Autowired
    private IPerfTestService perfTestService;

    /**
     * global influxdb bucket
     */
    @Value("${influxdb.bucket:default}")
    private String bucket;

    @GetMapping("/metrics")
    public MonitorModel<RootMetricNode> getMonitorInfo(MonitorHostDTO monitorHostDTO) {
        LOGGER.debug("Monitor param:{}", monitorHostDTO);
        MonitorModel<RootMetricNode> monitorModel = new MonitorModel<>();
        if (monitorHostDTO == null
            || StringUtils.isEmpty(monitorHostDTO.getService())
            || StringUtils.isEmpty(monitorHostDTO.getServiceInstance())
            || StringUtils.isEmpty(monitorHostDTO.getStartTime())) {
            monitorModel.setSuccess(false);
            monitorModel.setMessage("Exists invalid parameter.");
            LOGGER.error("Exists invalid parameter.");
            return monitorModel;
        }
        monitorHostDTO.setBucket(bucket);
        RootMetricNode allMonitorData = monitorService.getAllMonitorData(monitorHostDTO);
        monitorModel.setSuccess(true);
        monitorModel.setData(allMonitorData);
        return monitorModel;
    }

    /**
     * 查询需要监控的主机是否需要获取jvm数据
     *
     * @param monitorHostDTO 查询传入的参数信息
     * @param monitorHosts   系统查询到的主机信息
     */
    private void initJvmConfig(MonitorHostDTO monitorHostDTO, JSONArray monitorHosts) {
        if (monitorHostDTO == null || StringUtils.isEmpty(monitorHostDTO.getHost()) || StringUtils.isEmpty(monitorHostDTO.getIp())) {
            throw new RuntimeException("Host or ip can not be empty for monitoring.");
        }
        if (monitorHosts == null || monitorHosts.isEmpty()) {
            throw new RuntimeException("Can not found this host in config of this test.");
        }
        for (int i = 0; i < monitorHosts.size(); i++) {
            JSONObject monitorHost = monitorHosts.getJSONObject(i);
            if (!monitorHostDTO.getHost().equals(monitorHost.getString(MonitorHostKey.HOST.getServerKey()))) {
                continue;
            }
            if (!monitorHostDTO.getIp().equals(monitorHost.getString(MonitorHostKey.IP.getServerKey()))) {
                continue;
            }
            Boolean isMonitorJvm = monitorHost.getBoolean(MonitorHostKey.IS_MONITOR_JVM.getServerKey());
            String jvmType = monitorHost.getString(MonitorHostKey.JVM_TYPE.getServerKey());
            monitorHostDTO.setJvmType(jvmType);
            monitorHostDTO.setIsMonitorJvm(isMonitorJvm);
            break;
        }
    }
}
