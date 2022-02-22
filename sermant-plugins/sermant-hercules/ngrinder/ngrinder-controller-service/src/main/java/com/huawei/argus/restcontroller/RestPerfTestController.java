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

package com.huawei.argus.restcontroller;

import com.alibaba.fastjson.JSONObject;
import com.huawei.argus.testreport.service.impl.TpsCalculateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.grinder.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.ngrinder.agent.service.AgentManagerService;
import org.ngrinder.common.constants.GrinderConstants;
import org.ngrinder.common.util.FileDownloadUtils;
import org.ngrinder.infra.config.Config;
import org.ngrinder.infra.logger.CoreLogger;
import org.ngrinder.infra.spring.RemainedPath;
import org.ngrinder.model.MonitoringHost;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.Role;
import org.ngrinder.model.Status;
import org.ngrinder.model.User;
import org.ngrinder.perftest.service.AgentManager;
import org.ngrinder.perftest.service.PerfTestRunnable;
import org.ngrinder.perftest.service.PerfTestService;
import org.ngrinder.perftest.service.TagService;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.ngrinder.common.util.CollectionUtils.buildMap;
import static org.ngrinder.common.util.CollectionUtils.newHashMap;
import static org.ngrinder.common.util.ExceptionUtils.processException;
import static org.ngrinder.common.util.ObjectUtils.defaultIfNull;
import static org.ngrinder.common.util.Preconditions.checkArgument;
import static org.ngrinder.common.util.Preconditions.checkNotEmpty;
import static org.ngrinder.common.util.Preconditions.checkNotNull;
import static org.ngrinder.common.util.Preconditions.checkState;

@Api(tags = "PerfTest管理")
@RestController
@RequestMapping("/rest/api/")
public class RestPerfTestController extends RestBaseController {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestPerfTestController.class);

    @Autowired
    private PerfTestService perfTestService;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AgentManagerService agentManagerService;

    @Autowired
    private TagService tagService;

    @Autowired
    private PerfTestRunnable perfTestRunnable;

    /**
     * Get the perf test lists.
     *
     * @param user        user
     * @param query       query string to search the perf test
     * @param tag         tag
     * @param queryFilter "F" means get only finished, "S" means get only scheduled tests.
     * @param pageable    page
     * @return perftest/list
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测任务列表")
    @RequestMapping(value = {"/perftests"}, method = RequestMethod.GET)
    public Page<PerfTest> getAll(User user, @RequestParam(required = false) String query,
                                 @RequestParam(required = false) String tag, @RequestParam(required = false) String queryFilter,
                                 @PageableDefault(page = 0, size = 10) Pageable pageable,
                                 @RequestParam(required = false) String testName,
                                 @RequestParam(required = false) String testType,
                                 @RequestParam(required = false) String scriptPath,
                                 @RequestParam(required = false) String owner) {
        Sort sortById = defaultIfNull(pageable.getSort(), Sort.by(Sort.Direction.DESC, "id"));
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortById);
        Page<PerfTest> tests = perfTestService.getPagedAll(user, query, tag, queryFilter, pageable, testName, testType, scriptPath, owner);
        if (tests.getNumberOfElements() == 0) {
            pageable = PageRequest.of(0, pageable.getPageSize(), sortById);
            tests = perfTestService.getPagedAll(user, query, tag, queryFilter, pageable, testName, testType, scriptPath, owner);
        }
        return tests;
    }

    /**
     * 查询压测任务的标签
     *
     * @param user  当前用户
     * @param query 查询关键字
     * @return 获取所有标签
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测任务标签列表")
    @RequestMapping(value = {"/tags"}, method = RequestMethod.GET)
    public List<String> getAllTags(User user, @RequestParam(required = false) String query) {
        List<String> allTags = tagService.getAllTagStringsByKeywords(user, query);
        return allTags == null ? Collections.emptyList() : allTags;
    }

    /**
     * Get the perf test detail on the given perf test id.
     *
     * @param user user
     * @param id   perf test id
     * @return perftest/detail
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取指定压测任务明细")
    @RequestMapping(value = "/perftest/{id}", method = RequestMethod.GET)
    public PerfTest getOne(User user, @PathVariable Long id) {
        return getOneWithPermissionCheck(user, id, true);
    }

    /**
     * Search tags based on the given query.
     *
     * @param user  user to search
     * @param query query string
     * @return found tag list in json
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "搜索标签")
    @RequestMapping("/search/tag")
    public HttpEntity<String> searchTag(User user, @RequestParam(required = false) String query) {
        List<String> allStrings = tagService.getAllTagStrings(user, query);
        if (StringUtils.isNotBlank(query)) {
            allStrings.add(query);
        }
        return toJsonHttpEntity(allStrings);
    }

    /**
     * Create a new test.
     *
     * @param user     user
     * @param perfTest {@link PerfTest}
     * @return redirect:/perftest/list
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "POST", value = "创建压测任务")
    @RequestMapping(value = "/perftest", method = RequestMethod.POST)
    public PerfTest saveOne(User user, @RequestBody PerfTest perfTest) {
        validate(user, null, perfTest);
        // Point to the head revision
        perfTest.setTestName(StringUtils.trimToEmpty(perfTest.getTestName()));
        perfTest.setScriptRevision(1L);
        perfTest.setCreatedUser(user);
        perfTest.setUserId(user.getUserId());
        Set<MonitoringHost> monitoringHosts = perfTest.getMonitoringHosts();
        if (monitoringHosts != null && !monitoringHosts.isEmpty()) {
            for (MonitoringHost monitoringHost : monitoringHosts) {
                monitoringHost.setPerfTest(perfTest);
                monitoringHost.setId(null);
            }
        }
        return perfTestService.save(user, perfTest);
    }

    /**
     * start a test.
     *
     * @param user   user
     * @param testId 启动的压测任务id
     * @return perf_test
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "PUT", value = "启动压测任务")
    @RequestMapping(value = "/perftest/{testId}", method = RequestMethod.PUT)
    public Map<String, Object> startOne(User user, @PathVariable Long testId) {
        PerfTest perfTestExisted = perfTestService.getOne(testId);
        if (perfTestExisted == null) {
            LOGGER.error("Can not start test didn't exist, id={}.", testId);
            return returnError();
        }
        if (user == null || !user.getUserId().equals(perfTestExisted.getCreatedUser().getUserId())) {
            LOGGER.error("Can not start test, need created user to start.");
            return returnError();
        }
        if (perfTestExisted.getStatus() == Status.SAVED) {
            perfTestExisted.setStatus(Status.READY);
            perfTestService.save(user, perfTestExisted);
        }
        if (perfTestExisted.getStatus() != Status.READY) {
            LOGGER.error("Can not start test, because status isn't READY.");
            return returnError();
        }
        perfTestRunnable.doStart(user, testId);
        return returnSuccess();
    }

    @SuppressWarnings("ConstantConditions")
    private void validate(User user, PerfTest oldOne, PerfTest newOne) {
        if (oldOne == null) {
            oldOne = new PerfTest();
            oldOne.init();
        }
        newOne = oldOne.merge(newOne);
        checkNotEmpty(newOne.getTestName(), "testName should be provided");
        checkArgument(newOne.getStatus().equals(Status.READY) || newOne.getStatus().equals(Status.SAVED),
            "status only allows SAVE or READY");
        if (newOne.isThresholdRunCount()) {
            final Integer runCount = newOne.getRunCount();
            checkArgument(runCount > 0 && runCount <= agentManager
                    .getMaxRunCount(),
                "runCount should be equal to or less than %s", agentManager.getMaxRunCount());
        } else {
            final Long duration = newOne.getDuration();
            checkArgument(duration > 0 && duration <= (((long) agentManager.getMaxRunHour()) *
                    3600000L),
                "duration should be equal to or less than %s", agentManager.getMaxRunHour());
        }
        Map<String, MutableInt> agentCountMap = agentManagerService.getAvailableAgentCountMap(user);
        MutableInt agentCountObj = agentCountMap.get(isClustered() ? newOne.getRegion() : Config.NONE_REGION);
        checkNotNull(agentCountObj, "region should be within current region list");
        int agentMaxCount = agentCountObj.intValue();
        checkArgument(newOne.getAgentCount() <= agentMaxCount, "test agent should be equal to or less than %s",
            agentMaxCount);
        if (newOne.getStatus().equals(Status.READY)) {
            checkArgument(newOne.getAgentCount() >= 1, "agentCount should be more than 1 when it's READY status.");
        }

        checkArgument(newOne.getVuserPerAgent() <= agentManager.getMaxVuserPerAgent(),
            "vuserPerAgent should be equal to or less than %s", agentManager.getMaxVuserPerAgent());
        if (getConfig().isSecurityEnabled() && GrinderConstants.GRINDER_SECURITY_LEVEL_NORMAL.equals(getConfig().getSecurityLevel())) {
            checkArgument(StringUtils.isNotEmpty(newOne.getTargetHosts()),
                "targetHosts should be provided when security mode is enabled");
        }
        if (newOne.getStatus() != Status.SAVED) {
            checkArgument(StringUtils.isNotBlank(newOne.getScriptName()), "scriptName should be provided.");
        }
    }

    private Long[] convertString2Long(String ids) {
        String[] numbers = StringUtils.split(ids, ",");
        Long[] id = new Long[numbers.length];
        int i = 0;
        for (String each : numbers) {
            id[i++] = NumberUtils.toLong(each, 0);
        }
        return id;
    }

    private List<Map<String, Object>> getStatus(List<PerfTest> perfTests) {
        List<Map<String, Object>> statuses = newArrayList();
        for (PerfTest each : perfTests) {
            Map<String, Object> result = newHashMap();
            result.put("id", each.getId());
            result.put("status_id", each.getStatus());
            result.put("status_type", each.getStatus());
            result.put("name", getMessages(each.getStatus().getSpringMessageKey()));
            result.put("icon", each.getStatus().getIconName());
            result.put("message",
                StringUtils.replace(each.getProgressMessage() + "\n<b>" + each.getLastProgressMessage() + "</b>\n"
                    + each.getLastModifiedDateToStr(), "\n", "<br/>"));
            result.put("deletable", each.getStatus().isDeletable());
            result.put("stoppable", each.getStatus().isStoppable());
            result.put("reportable", each.getStatus().isReportable());
            statuses.add(result);
        }
        return statuses;
    }


    /**
     * Delete the perf tests having given IDs.
     *
     * @param user user
     * @param ids  comma operated IDs
     * @return success json messages if succeeded.
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "DELETE", value = "批量删除压测任务")
    @RequestMapping(value = "/perftest", method = RequestMethod.DELETE)
    public HttpEntity<JSONObject> delete(User user, @RequestParam(value = "ids", defaultValue = "") String ids) {
        for (String idStr : StringUtils.split(ids, ",")) {
            perfTestService.delete(user, NumberUtils.toLong(idStr, 0));
        }
        return successJsonHttpEntity();
    }

    @ApiOperation(tags = "PerfTest管理", httpMethod = "DELETE", value = "批量删除压测任务报告")
    @RequestMapping(value = "/report", method = RequestMethod.DELETE)
    public HttpEntity<JSONObject> deleteReportFile(User user, @RequestParam(value = "ids", defaultValue = "") String ids) {
        for (String idStr : StringUtils.split(ids, ",")) {
            perfTestService.deleteReportFile(user, NumberUtils.toLong(idStr, 0));
        }
        return successJsonHttpEntity();
    }

    /**
     * Stop the perf tests having given IDs.
     *
     * @param user user
     * @param ids  comma separated perf test IDs
     * @return success json if succeeded.
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "PUT", value = "停止压测任务")
    @RequestMapping(value = "/stop/perftest", method = RequestMethod.PUT)
    public HttpEntity<JSONObject> stop(User user, @RequestParam(value = "ids", defaultValue = "") String ids) {
        for (String idStr : StringUtils.split(ids, ",")) {
            perfTestService.stop(user, NumberUtils.toLong(idStr, 0));
        }
        return successJsonHttpEntity();
    }

    private JSONObject getPerfGraphData(Long id, String[] dataTypes, boolean onlyTotal, int imgWidth) {
        final PerfTest test = perfTestService.getOne(id);
        int interval = perfTestService.getReportDataInterval(id, dataTypes[0], imgWidth);
        JSONObject result = new JSONObject();
        for (String each : dataTypes) {
            Pair<ArrayList<String>, ArrayList<String>> tpsResult = perfTestService.getReportData(id, each, onlyTotal, interval);
            Map<String, Object> dataMap = Maps.newHashMap();
            dataMap.put("labels", tpsResult.getFirst());
            dataMap.put("data", tpsResult.getSecond());
            result.put(StringUtils.replaceChars(each, "()", ""), dataMap);
        }
        result.put(PARAM_TEST_CHART_INTERVAL, interval * test.getSamplingInterval());
        return result;
    }


    /**
     * Get the running division in perftest configuration page.
     *
     * @param user user
     * @param id   test id
     * @return perftest/running
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测任务的分配配置信息")
    @RequestMapping(value = "/perftest/{id}/running_div")
    public JSONObject getReportSection(User user, @PathVariable long id) {
        PerfTest test = getOneWithPermissionCheck(user, id, false);
        JSONObject modelInfos = new JSONObject();
        modelInfos.put(PARAM_TEST, test);
        return modelInfos;
    }

    /**
     * 根据压测任务id获取指定任务的压测数据
     *
     * @param user         调用接口用户信息
     * @param id           压测任务id
     * @param imgWidth     动图展示宽度
     * @param thisDuration 动图展示区间
     * @param timeInterval 动图展示频率
     * @return 压测任务搜集的数据信息
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取当前时间之前一段时间压测任务的TPS数据")
    @RequestMapping(value = "/report/basic")
    public JSONObject getReportSectionById(User user, @RequestParam long id, @RequestParam int imgWidth, @RequestParam int thisDuration, @RequestParam int timeInterval) {
        PerfTest test = getOneWithPermissionCheck(user, id, false);
        int interval = perfTestService.getReportDataInterval(id, "TPS", imgWidth);
        JSONObject modelInfos = new JSONObject();
        modelInfos.put(PARAM_LOG_LIST, perfTestService.getLogFiles(id));
        int sampleInterval = interval * test.getSamplingInterval();
        modelInfos.put(PARAM_TEST_CHART_INTERVAL, sampleInterval);
        modelInfos.put(PARAM_TEST, test);
        TpsCalculateService tpsCalculateService = new TpsCalculateService();
        Date startTime = test.getStartTime();
        Date finishTime = test.getFinishTime();
        tpsCalculateService.setResultSampleInterval(timeInterval)
            .setTestSampleInterval(test.getSamplingInterval())
            .setResultShowTime(thisDuration)
            .setNeededExecuteTime(test.getDuration())
            .setTestStartTime(startTime == null ? 0 : startTime.getTime())
            .setTestEndTime(finishTime == null ? 0 : finishTime.getTime())
            .isRunning(test.getStatus() == Status.TESTING)
            .setTpsOriginalData(perfTestService.getSingleReportDataAsJson(id, "TPS", interval));
        modelInfos.put(PARAM_TPS, tpsCalculateService.sampleData());
        return modelInfos;
    }

    /**
     * Download the CSV report for the given perf test id.
     *
     * @param user     user
     * @param id       test id
     * @param response response
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "下载压测任务的CSV格式报告")
    @RequestMapping(value = "/{id}/download_csv")
    public void downloadCSV(User user, @PathVariable("id") long id, HttpServletResponse response) {
        PerfTest test = getOneWithPermissionCheck(user, id, false);
        File targetFile = perfTestService.getCsvReportFile(test);
        checkState(targetFile.exists(), "File %s doesn't exist!", targetFile.getName());
        FileDownloadUtils.downloadFile(response, targetFile);
    }

    /**
     * Download logs for the perf test having the given id.
     *
     * @param user     user
     * @param id       test id
     * @param path     path in the log folder
     * @param response response
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "下载压测任务日志方式1")
    @RequestMapping(value = "/{id}/download_log/**")
    public void downloadLog(User user, @PathVariable("id") long id, @RemainedPath String path,
                            HttpServletResponse response) {
        getOneWithPermissionCheck(user, id, false);
        File targetFile = perfTestService.getLogFile(id, path);
        FileDownloadUtils.downloadFile(response, targetFile);
    }

    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "下载压测任务日志方式2")
    @RequestMapping(value = "/download_log")
    public JSONObject downloadLogByID(User user, @RequestParam long id, @RequestParam String path) {
        getOneWithPermissionCheck(user, id, false);
        File targetFile = perfTestService.getLogFile(id, path);
        JSONObject logFile = new JSONObject();
        if (targetFile == null) {
            logFile.put(JSON_SUCCESS, false);
            return logFile;
        }
        try {
            logFile.put(
                "Content-Disposition",
                "attachment;filename=" + targetFile.getName());
            logFile.put("contentType", "application/octet-stream; charset=UTF-8");
            logFile.put("Content-Length", "" + targetFile.length());
            ByteArrayInputStream fis = null;
            logFile.put("content", Files.readAllBytes(targetFile.toPath()));
            logFile.put(JSON_SUCCESS, true);
        } catch (IOException e) {
            CoreLogger.LOGGER.error("Error while download log. {}", logFile, e);
        }

        return logFile;
    }

    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测任务实时TPS数据")
    @RequestMapping(value = "/report/sample")
    public JSONObject refreshTestRunningById(User user, @RequestParam long id) {
        PerfTest test = checkNotNull(getOneWithPermissionCheck(user, id, false), "given test should be exist : " + id);
        JSONObject response = new JSONObject();
        response.put("test", test);
        response.put("status", test.getStatus());
        response.put("perf", perfTestService.getStatistics(test));
        response.put("agent", perfTestService.getAgentStat(test));
        response.put("monitor", perfTestService.getMonitorStat(test));
        response.put(PARAM_LOG_LIST, perfTestService.getLogFiles(id));
        return response;
    }

    /**
     * Get the detailed perf test report.
     *
     * @param id test id
     * @return perftest/detail_report
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测任务的压测报告明细")
    @RequestMapping(value = {"/report/detail"})
    public JSONObject getReportById(@RequestParam long id) {
        JSONObject modelInfos = new JSONObject();
        PerfTest test = perfTestService.getOne(id);
        if (test == null) {
            return modelInfos;
        }
        modelInfos.put("test", test);
        modelInfos.put("plugins", perfTestService.getAvailableReportPlugins(id));
        return modelInfos;
    }

    private PerfTest getOneWithPermissionCheck(User user, Long id, boolean withTag) {
        PerfTest perfTest = withTag ? perfTestService.getOneWithTag(id) : perfTestService.getOne(id);
        if (perfTest == null) {
            throw processException("User " + user.getUserId() + " has no PerfTest " + id);
        }
        if (user.getRole().equals(Role.ADMIN) || user.getRole().equals(Role.SUPER_USER)) {
            return perfTest;
        }
        if (!user.equals(perfTest.getCreatedUser())) {
            throw processException("User " + user.getUserId() + " has no right on PerfTest " + id);
        }
        return perfTest;
    }

    /**
     * Get the count of currently running perf test and the detailed progress info for the given perf test IDs.
     *
     * @param user user
     * @param ids  comma separated perf test list
     * @return JSON message containing perf test status
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测任务的状态")
    @RequestMapping("/status")
    public HttpEntity<String> getStatuses(User user, @RequestParam(value = "ids", defaultValue = "") String ids) {
        List<PerfTest> perfTests = perfTestService.getAll(user, convertString2Long(ids));
        return toJsonHttpEntity(buildMap("perfTestInfo", perfTestService.getCurrentPerfTestStatistics(), "status",
            getStatus(perfTests)));
    }

    /**
     * Get the detailed report graph data for the given perf test id.
     * This method returns the appropriate points based on the given imgWidth.
     *
     * @param id       test id
     * @param dataType which data
     * @param imgWidth imageWidth
     * @return json string.
     */
    @ApiOperation(tags = "PerfTest管理", httpMethod = "GET", value = "获取压测报告性能数据")
    @RequestMapping({"/report/perf"})
    public JSONObject getPerfGraphById(@RequestParam("id") long id,
                                       @RequestParam(defaultValue = "") String dataType,
                                       @RequestParam(defaultValue = "false") boolean onlyTotal,
                                       @RequestParam int imgWidth) {
        String[] dataTypes = checkNotEmpty(StringUtils.split(dataType, ","), "dataType argument should be provided");
        PerfTest perfTest = perfTestService.getOne(id);
        if (perfTest == null) {
            return new JSONObject();
        }
        return getPerfGraphData(id, dataTypes, onlyTotal, imgWidth);
    }
}
