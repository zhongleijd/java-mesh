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

package com.huawei.argus.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.huawei.argus.report.service.PerfTestReportService;
import org.ngrinder.common.controller.BaseController;
import org.ngrinder.model.PerfTestReport;
import org.ngrinder.model.User;
import org.ngrinder.perftest.service.report.SearchReportDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.ngrinder.common.util.ObjectUtils.defaultIfNull;

@SuppressWarnings("SpringJavaAutowiringInspection")
@Controller
@RequestMapping("/rest/report")
public class PerfTestReportController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerfTestReportController.class);

    @Autowired
    private PerfTestReportService perfTestReportService;

    @Autowired
    private SearchReportDataService searchReportDataService;

    @RequestMapping(value = {"", "/"}, method = RequestMethod.GET)
    public @ResponseBody
    Page<PerfTestReport> getBasicReport(User user,
                                        @PageableDefault(page = 0, size = 10) Pageable pageable,
                                        @RequestParam(required = false) String query) {

        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            defaultIfNull(pageable.getSort(), Sort.by(Sort.Direction.DESC, "id")));
        return perfTestReportService.getBasicReportByUserId(pageable, user, query);
    }


    @RequestMapping(value = {"/graph/{id}", "/{id}/"}, method = RequestMethod.GET)
    public @ResponseBody
    JSONObject getGraphReportByReportId(@PathVariable Long id) {
        //根据报告id查询当前的图表数据
        return perfTestReportService.getPerfGraphDataByReportId(id);
    }

    @RequestMapping(value = {"/basic/{id}", "/{id}/"}, method = RequestMethod.GET)
    public @ResponseBody
    JSONObject getBasicReportByReportId(@PathVariable Long id) {
        return perfTestReportService.getAllReportByReportId(id);
    }


    @RequestMapping(value = {"/{id}", "/{id}/"}, method = RequestMethod.DELETE)
    public @ResponseBody
    Object deleteBasicReportByReportId(@PathVariable Long id) {
        perfTestReportService.deleteBasicReportByReportId(id);
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/hosts/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String[] getTargetHosts(User user, @PathVariable(value = "id") long id) {
        PerfTestReport thisPerfTestReport = perfTestReportService.getBasicReportByReportId(id);
        List<String> lstHosts = thisPerfTestReport.getTargetHostIP();
        return lstHosts.toArray(new String[lstHosts.size()]);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Map<String, List<Map<String, String>>>> getReportDataByTime(
        @PathVariable(value = "id") long id,
        @RequestParam(name = "startTime") String startTimeString,
        @RequestParam(name = "endTime") String endTimeString) {
        SimpleDateFormat dateFormatEndWithSeconds = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date startDateTime = dateFormatEndWithSeconds.parse(startTimeString);
            Date endDateTime = dateFormatEndWithSeconds.parse(endTimeString);
            return searchReportDataService.getReportDataDetail(id, startDateTime, endDateTime);
        } catch (ParseException e) {
            LOGGER.error("Invalid time format was found: startTime:{}, endTime:{}", startTimeString, endTimeString);
            return Collections.emptyMap();
        }
    }
}
