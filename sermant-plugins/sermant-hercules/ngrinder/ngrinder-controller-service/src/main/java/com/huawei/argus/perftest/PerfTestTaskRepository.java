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

import org.ngrinder.model.PerfScene;
import org.ngrinder.model.PerfTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface PerfTestTaskRepository extends JpaRepository<PerfTest, Long>, JpaSpecificationExecutor<PerfTest> {

    Page<PerfTest> findAll(Specification<PerfTest> spec, Pageable pageable);

    @Query(value = "SELECT hostName FROM AGENT WHERE id in ?1", nativeQuery = true)
    List<String> getSelectAgentNameList(List<String> agentIds);

    @Query(value = "SELECT id FROM PERF_TEST_REPORT WHERE perf_test_id = ?1 order by created_date desc limit 0,1", nativeQuery = true)
    String getReportIdByTestId(Long id);

    /**
     * 查找引用了该压测场景的压测任务
     *
     * @param perfScene
     * @return
     */
    @Query(value = "select * from PERF_TEST where scene_id=:#{perfScene.id}", nativeQuery = true)
    List<PerfTest> getPerfTestsByPerfScene(PerfScene perfScene);
}
