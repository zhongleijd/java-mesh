/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Based on com/alibaba/csp/sentinel/dashboard/entity/rule/FlowRuleEntity.java
 * from the Alibaba Sentinel project.
 */

package com.huawei.flowcontrol.console.entity;

import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 此处部分引用alibaba/Sentinel开源社区代码，诚挚感谢alibaba/Sentinel开源团队的慷慨贡献
 */
@Getter
@Setter
public abstract class BaseRule<T extends AbstractRule> {
    private Long id;
    private String app;
    private String ip;
    private Integer port;
    private Date gmtCreate;
    private Date gmtModified;
    private String extInfo;
    protected String resource;
    protected String limitApp;

    public abstract T toRule();
}
