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

package com.huawei.argus.report.service;


import com.alibaba.fastjson.JSONObject;
import com.huawei.argus.report.repository.PerfTestReportRepository;
import net.grinder.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.agent.repository.AgentManagerRepository;
import org.ngrinder.common.controller.BaseController;
import org.ngrinder.model.AgentInfo;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.PerfTestReport;
import org.ngrinder.model.Role;
import org.ngrinder.model.User;
import org.ngrinder.perftest.service.PerfTestService;
import org.python.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.ngrinder.common.util.Preconditions.checkNotEmpty;

@Service
public class PerfTestReportService extends BaseController {

    public static final String PARAM_TEST_CHART_INTERVAL = "chartInterval";

    @Autowired
    private PerfTestService perfTestService;

    @Autowired
    private PerfTestReportRepository perfTestReportRepository;

    @Autowired
    private AgentManagerRepository agentManagerRepository;


    private final Properties mongodbProps = PropertiesLoaderUtils.loadProperties(new ClassPathResource("/mongoDB.properties"));

    public PerfTestReportService() throws IOException {
    }


    //获取并持久化压测报告的图表数据
    @SuppressWarnings("MVCPathVariableInspection")
    public void savePerfGraph(long id, PerfTestReport perfTestReport, String dataType, boolean onlyTotal, int imgWidth) {
        String[] dataTypes = checkNotEmpty(StringUtils.split(dataType, ","), "dataType argument should be provided");
        Map<String, Object> stringObjectMap = savePerfGraphData(id, perfTestReport, dataTypes, onlyTotal, imgWidth);
    }

    //获取并持久化压测任务的基本数据
    public PerfTestReport saveApiBasicReport(long id, int imgWidth) {
        //PerfTest perftest = getOneWithPermissionCheck(user, id, false);
        PerfTest perftest = getOneWithPermissionCheck(id, false);
        String runTime = perftest.getRuntimeStr();
        return save(perftest, runTime);
    }

    private Map<String, Object> savePerfGraphData(long id, PerfTestReport perfTestReport, String[] dataTypes, boolean onlyTotal, int imgWidth) {
        final PerfTest test = perfTestService.getOne(id);
        int interval = perfTestService.getReportDataInterval(id, dataTypes[0], imgWidth);
        Map<String, Object> resultMap = Maps.newHashMap();
        for (String each : dataTypes) {
            Pair<ArrayList<String>, ArrayList<String>> everyGraphResult = perfTestService.getReportData(id, each, onlyTotal, interval);
            Map<String, Object> dataMap = Maps.newHashMap();
            dataMap.put("labels", everyGraphResult.getFirst());
            dataMap.put("data", everyGraphResult.getSecond());
            resultMap.put(StringUtils.replaceChars(each, "()", ""), dataMap);
        }
        resultMap.put(PARAM_TEST_CHART_INTERVAL, interval * test.getSamplingInterval());
//		resultMap.put(PERF_TEST_REPORT_ID, perfTestReport.getId());
//		System.out.println("resultMap:"+resultMap);
        return resultMap;
    }


    private PerfTest getOneWithPermissionCheck(Long id, boolean withTag) {
        PerfTest perfTest = withTag ? perfTestService.getOneWithTag(id) : perfTestService.getOne(id);

        return perfTest;
    }

    public PerfTestReport save(PerfTest perfTest, String runTime) {

        PerfTestReport perfTestReport = new PerfTestReport();
        perfTestReport.setTestName(perfTest.getTestName());
        perfTestReport.setTagString(perfTest.getTagString());
        perfTestReport.setDescription(perfTest.getDescription());
        perfTestReport.setStatus(perfTest.getStatus());
        perfTestReport.setIgnoreSampleCount(perfTest.getIgnoreSampleCount());
        perfTestReport.setScheduledTime(perfTest.getScheduledTime());
        perfTestReport.setStartTime(perfTest.getStartTime());
        perfTestReport.setFinishTime(perfTest.getFinishTime());
        perfTestReport.setTargetHosts(perfTest.getTargetHosts());
        perfTestReport.setUseRampUp(perfTest.getUseRampUp());
        perfTestReport.setRampUpType(perfTest.getRampUpType());
        perfTestReport.setThreshold(perfTest.getThreshold());
        perfTestReport.setScriptName(perfTest.getScriptName());
        perfTestReport.setDuration(perfTest.getDuration());
        perfTestReport.setRunCount(perfTest.getRunCount());
        perfTestReport.setAgentCount(perfTest.getAgentCount());
        perfTestReport.setVuserPerAgent(perfTest.getVuserPerAgent());
        perfTestReport.setProcesses(perfTest.getProcesses());
        perfTestReport.setRampUpInitCount(perfTest.getRampUpInitCount());
        perfTestReport.setRampUpInitSleepTime(perfTest.getRampUpInitSleepTime());
        perfTestReport.setRampUpStep(perfTest.getRampUpStep());
        perfTestReport.setRampUpIncrementInterval(perfTest.getRampUpIncrementInterval());
        perfTestReport.setThreads(perfTest.getThreads());
        perfTestReport.setTests(perfTest.getTests());
        perfTestReport.setErrors(perfTest.getErrors());
        perfTestReport.setMeanTestTime(perfTest.getMeanTestTime());
        perfTestReport.setTps(perfTest.getTps());
        perfTestReport.setPeakTps(perfTest.getPeakTps());
        perfTestReport.setProgressMessage(perfTest.getProgressMessage());
        perfTestReport.setTestComment(perfTest.getTestComment());
        perfTestReport.setScriptRevision(perfTest.getScriptRevision());
        perfTestReport.setRegion(perfTest.getRegion());
        perfTestReport.setSamplingInterval(perfTest.getSamplingInterval());
        perfTestReport.setParam(perfTest.getParam());
        perfTestReport.setCreatedDate(perfTest.getCreatedDate());
        perfTestReport.setLastModifiedDate(perfTest.getLastModifiedDate());
        perfTestReport.setPerfTestId(perfTest.getId());
        perfTestReport.setRunTime(runTime);
        perfTestReport.setCreatedUser(perfTest.getCreatedUser());
        perfTestReport.setLastModifiedUser(perfTest.getLastModifiedUser());
        perfTestReport.setType(perfTest.getPerfScene() == null ? null : perfTest.getPerfScene().getType());
        perfTestReport.setAgentIds(perfTest.getAgentIds());

        return perfTestReportRepository.save(perfTestReport);
    }

    public Page<PerfTestReport> getApiBasicReport(Pageable pageable) {

        //Sort sort_id = new Sort(Sort.Direction.DESC, "id");
        //List<PerfTestReport> perfTestReports = perfTestReportRepository.findAll(sort_id);

        //注意！PerfTestReport中user是个对象，id为user的id，userId是名字:admin;
        Page<PerfTestReport> perfTestReports = perfTestReportRepository.findAll(pageable);
        return perfTestReports;
    }


    //根据userid分页查询
    public Page<PerfTestReport> getBasicReportByUserId(Pageable pageable, User user, String query) {

        Specification<PerfTestReport> spec = Specification.where(idEmptyPredicate());
        // User can see only his own test
        if (user.getRole().equals(Role.USER)) {
            spec = spec.and(createdBy(user));
        }
        if (StringUtils.isNotBlank(query)) {
            spec = spec.and(likeTestNameOrDescription(query));
        }

        Page<PerfTestReport> PerfTestReports = perfTestReportRepository.findAll(spec, pageable);
        return PerfTestReports;
    }

    private static Specification<PerfTestReport> idEmptyPredicate() {
        return new Specification<PerfTestReport>() {
            @Override
            public Predicate toPredicate(Root<PerfTestReport> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return root.get("id").isNotNull();
            }
        };
    }

    private static Specification<PerfTestReport> createdBy(final User user) {
        return new Specification<PerfTestReport>() {
            @Override
            public Predicate toPredicate(Root<PerfTestReport> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return cb.or(cb.equal(root.get("createdUser"), user));
            }
        };
    }

    public static Specification<PerfTestReport> likeTestNameOrDescription(final String queryString) {
        return new Specification<PerfTestReport>() {
            @Override
            public Predicate toPredicate(Root<PerfTestReport> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                String queryStr = ("%" + queryString + "%").toLowerCase();
                return cb.or(cb.like(cb.lower(root.get("testName").as(String.class)), queryStr),
                    cb.like(root.get("description").as(String.class), queryStr));
            }
        };
    }


    public PerfTestReport getBasicReportByReportId(Long id) {
        return perfTestReportRepository.findById(id).orElse(null);
    }

    public JSONObject getAllReportByReportId(Long id) {
        JSONObject jsonObject = new JSONObject();
        PerfTestReport thisPerfTestReport = perfTestReportRepository.findById(id).orElse(null);
        jsonObject.put("PerfTestReport", thisPerfTestReport);
        String agentIdsStr = thisPerfTestReport.getAgentIds();
        if (StringUtils.isNotBlank(agentIdsStr)) {
            String[] agentIdsArr = agentIdsStr.split(",");
            List<String> agentIdsList = Arrays.asList(agentIdsArr);
            Collection<AgentInfo> agentInfos = agentManagerRepository.getSelectedAgents(agentIdsList);
            jsonObject.put("agentInfo", agentInfos);
        } else {
            jsonObject.put("agentInfo", null);
        }


        return jsonObject;
    }


    public JSONObject getPerfGraphDataByReportId(Long id) {
        return new JSONObject();
    }

    ;

    public void deleteBasicReportByReportId(Long id) {
        //删除报告基本数据
        perfTestReportRepository.deleteById(id);
    }
}
