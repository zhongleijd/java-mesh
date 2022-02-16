/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.sermant.core.service.meta;

import com.huawei.sermant.core.config.common.BaseConfig;
import com.huawei.sermant.core.config.common.ConfigTypeKey;

/**
 * 配置当前服务信息
 *
 * @author zhouss
 * @since 2022-02-09
 */
@ConfigTypeKey("service.meta")
public class ServiceMeta implements BaseConfig {
    /**
     * app名称
     */
    private String application = "default";

    /**
     * 当前版本
     */
    private String version = "0.0.0";

    /**
     * 命名空间
     */
    private String project = "default";

    /**
     * 环境
     */
    private String environment = "development";

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
