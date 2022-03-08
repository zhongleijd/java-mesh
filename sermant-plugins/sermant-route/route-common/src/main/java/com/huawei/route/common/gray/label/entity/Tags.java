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

package com.huawei.route.common.gray.label.entity;

/**
 * 路由标签
 *
 * @author provenceee
 * @since 2021/10/23
 */
public class Tags {
    /**
     * 路由的应用版本
     */
    private String version;

    /**
     * appId
     */
    private String app;

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getApp() {
        return this.app;
    }
}
