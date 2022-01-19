/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.argus.perftest;

import org.ngrinder.model.PerfTest;
import org.ngrinder.model.User;

import java.io.File;
import java.util.List;

public interface IPerfTestTaskService {
    /**
     * 根据唯一id查询
     *
     * @param testId 任务id
     * @return 查询的任务id
     */
    PerfTest getOne(Long testId);

    /**
     * 查询测试任务脚本路径
     *
     * @param perfTest 测试任务
     * @return 测试任务脚本路径
     */
    File getPerfTestDirectory(PerfTest perfTest);

    /**
     * 获取查询的agent
     *
     * @param perfTest 根据测试任务获取agent
     * @return agent列表
     */
    List<String> getSelectAgentNameList(PerfTest perfTest);

    /**
     * 添加、修改之后的保存
     *
     * @param user     保存的用户
     * @param perfTest 保存的测试任务
     * @return 保存成功的测试任务
     */
    PerfTest save(User user, PerfTest perfTest);
}
