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

import org.apache.commons.io.FileUtils;
import org.ngrinder.infra.config.Config;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.Permission;
import org.ngrinder.model.Status;
import org.ngrinder.model.Tag;
import org.ngrinder.model.User;
import org.ngrinder.perftest.service.ConsoleManager;
import org.ngrinder.perftest.service.TagService;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.service.NfsFileEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;

import static org.ngrinder.common.util.Preconditions.checkNotNull;
import static org.ngrinder.model.Status.READY;

@Service
public class PerfTestTaskService implements IPerfTestTaskService {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PerfTestTaskService.class);

    @Autowired
    private PerfTestTaskRepository perfTestTaskRepository;

    @Autowired
    private Config config;

    @Autowired
    private ConsoleManager consoleManager;

    @Autowired
    private NfsFileEntryService fileEntryService;

    @Autowired
    private TagService tagService;

    @Override
    public PerfTest getOne(Long testId) {
        return perfTestTaskRepository.findById(testId).orElse(null);
    }

    @Override
    public File getPerfTestDirectory(PerfTest perfTest) {
        return config.getHome().getPerfTestDirectory(perfTest);
    }

    @Override
    @Transactional
    public PerfTest save(User user, PerfTest perfTest) {
        try {
            perfTest.setScriptName(fileEntryService.getSpecifyScript(user, perfTest).getPath());
        } catch (IOException e) {
            LOGGER.error("The script for test is not exist:{}", perfTest.getTestName());
            return null;
        }
        try {
            attachFileRevision(perfTest);
        } catch (IOException e) {
            perfTest.setScriptRevision(1L);
        }
        attachTags(user, perfTest, perfTest.getTagString());
        if (perfTest.getStatus().equals(READY)) {
            perfTest.clearMessages();
            deletePerfTestDirectory(perfTest);
        }

        try {
            return save(perfTest);
        } catch (Throwable throwable) {
            return null;
        }
    }

    @Override
    public List<String> getSelectAgentNameList(PerfTest perfTest) {
        String agentIds = perfTest.getAgentIds();
        String[] split = agentIds.split(",");
        List<String> ids = Arrays.asList(split);
        List<String> selectAgentNameList = perfTestTaskRepository.getSelectAgentNameList(ids);

        LOGGER.info("selectAgentNameList:{}", selectAgentNameList);
        return selectAgentNameList;
    }

    //	@Override
    @Transactional
    public void stop(User user, Long id) {
        PerfTest perfTest = getOne(id);
        // If it's not requested by user who started job. It's wrong request.
        if (!hasPermission(perfTest, user, Permission.STOP_TEST_OF_OTHER)) {
            return;
        }
        // If it's not stoppable status.. It's wrong request.
        if (!perfTest.getStatus().isStoppable()) {
            return;
        }
        // Just mark cancel on console
        // This will be not be effective on cluster mode.
        consoleManager.getConsoleUsingPort(perfTest.getPort()).cancel();
        perfTest.setStopRequest(true);
        perfTestTaskRepository.save(perfTest);
    }

    public boolean hasPermission(PerfTest perfTest, User user, Permission type) {
        return perfTest != null && (user.getRole().hasPermission(type) || user.equals(perfTest.getCreatedUser()));
    }

    private void deletePerfTestDirectory(PerfTest perfTest) {
        FileUtils.deleteQuietly(getPerfTestDirectory(perfTest));
    }

    private void attachFileRevision(PerfTest perfTest) throws IOException {
        if (perfTest.getStatus() == Status.READY) {
            FileEntry scriptEntry = fileEntryService.getSpecifyScript(perfTest);
            long revision = scriptEntry != null ? scriptEntry.getRevision() : -1;
            perfTest.setScriptRevision(revision);
        }
    }

    private void attachTags(User user, PerfTest perfTest, String tagString) {
        SortedSet<Tag> tags = tagService.addTags(user,
            org.apache.commons.lang.StringUtils.split(org.apache.commons.lang.StringUtils.trimToEmpty(tagString), ","));
        perfTest.setTags(tags);
        perfTest.setTagString(buildTagString(tags));
    }

    private String buildTagString(Set<Tag> tags) {
        List<String> tagStringResult = new ArrayList<String>();
        for (Tag each : tags) {
            tagStringResult.add(each.getTagValue());
        }
        return org.apache.commons.lang.StringUtils.join(tagStringResult, ",");
    }

    private PerfTest save(PerfTest perfTest) throws Throwable {
        checkNotNull(perfTest);
        // Merge if necessary
        if (perfTest.exist()) {
            PerfTest existingPerfTest = perfTestTaskRepository.findById(perfTest.getId()).orElseThrow(
                (Supplier<Throwable>) () -> new IllegalArgumentException("The perf test not exist.")
            );
            perfTest = existingPerfTest.merge(perfTest);
        } else {
            perfTest.clearMessages();
        }
        if (perfTest.getStopRequest() != null && perfTest.getStopRequest()) {
            perfTest.setStopRequest(Boolean.FALSE);
        }

        return perfTestTaskRepository.saveAndFlush(perfTest);
    }

}
