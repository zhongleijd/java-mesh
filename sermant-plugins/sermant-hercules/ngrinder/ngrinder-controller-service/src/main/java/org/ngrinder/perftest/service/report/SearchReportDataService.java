/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ngrinder.perftest.service.report;

import org.apache.commons.lang.StringUtils;
import org.ngrinder.perftest.service.PerfTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述：提供根据时间段查询压测数据的能力
 *
 * @author zl
 * @since 2022-03-23
 */
@Service
public class SearchReportDataService {
    /**
     * 日志工具
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchReportDataService.class);

    /**
     * 压测数据汇总文件名称
     */
    private static final String REPORT_CSV = "output.csv";

    /**
     * 压测数据中V user的列名
     */
    private static final String VUSER_KEY_NAME = "vuser";

    /**
     * 压测数据中测试description的列名
     */
    private static final String TEST_DESCRIPTION_KEY_NAME = "Description";

    /**
     * 测试项中，汇总数据Total的key
     */
    private static final String TOTAL_METRIC_KEY_NAME = "Total";

    /**
     * 压测数据中DateTime的列名
     */
    private static final String DATE_TIME_KEY_NAME = "DateTime";

    /**
     * 指标名称
     */
    private static final String[] metrics = {
        VUSER_KEY_NAME,
        "Tests",
        "Errors",
        "Mean_Test_Time_(ms)",
        "Test_Time_Standard_Deviation_(ms)",
        "TPS",
        "Mean_response_length",
        "Response_bytes_per_second",
        "Response_errors",
        "Mean_time_to_resolve_host",
        "Mean_time_to_establish_connection",
        "Mean_time_to_first_byte"
    };

    @Autowired
    private PerfTestService perfTestService;

    /**
     * 获取指定时间段内的压测数据
     *
     * @param testId    压测任务id
     * @param startTime 开始时间
     * @param endTime   截止时间
     * @return 压测数据
     */
    public Map<String, Map<String, List<Map<String, String>>>> getReportDataDetail(long testId, Date startTime, Date endTime) {
        // 获取压测任务测试数据所在文件夹
        File reportFileDirectory = perfTestService.getReportFileDirectory(testId);
        try (FileReader reportReader = new FileReader(new File(reportFileDirectory, REPORT_CSV));
             BufferedReader bufferedReportReader = new BufferedReader(reportReader)) {
            // 第一行是标题行
            String titleLine = bufferedReportReader.readLine();

            // 取第二行作为内容行，来获取压测任务的描述信息，即Description字段的值
            String eachContentLine = bufferedReportReader.readLine();

            // 获取每一个字段的标题以及内容在被“,”分隔之后的数组中的索引
            Map<String, Integer> titleMap = getTitleMap(titleLine);

            // 根据内容和标题把压测任务中的每一个测试项统计出来
            List<ReportTest> tests = getTest(titleLine, eachContentLine);

            // 获取测试数据框架信息，便于向数据容器中添加数据
            Map<String, List<ReportTest>> dataFrame = getDataFrame(tests);

            // 获取测试数据容器，根据上一步的框架直接填充数据到容器中
            Map<String, Map<String, List<Map<String, String>>>> containerFrame = getContainerFrame(tests);
            while (eachContentLine != null) {
                // 开始处理每一行数据，这里因为上面已经获取了一行内容，所以没有使用eachContentLine = bufferedReportReader.readLine()
                String[] contentArray = eachContentLine.split(",");
                String dateTime = getDateTime(contentArray, titleMap);
                if (!validTime(dateTime, startTime, endTime)) {
                    eachContentLine = bufferedReportReader.readLine();
                    continue;
                }
                handleContentLine(contentArray, containerFrame, dataFrame, titleMap);
                eachContentLine = bufferedReportReader.readLine();
            }
            return containerFrame;
        } catch (FileNotFoundException e) {
            LOGGER.error("The report file not found.");
            return Collections.emptyMap();
        } catch (IOException exception) {
            LOGGER.error("The report file exist, but occur an error when read it.");
            return Collections.emptyMap();
        }
    }

    /**
     * 判断当前行时间数据是否合法
     *
     * @param dateTime  需要判断的时间点
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 合法返回true，不合法返回false
     */
    private boolean validTime(String dateTime, Date startTime, Date endTime) {
        SimpleDateFormat dateFormatEndWithSeconds = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date datePoint = dateFormatEndWithSeconds.parse(dateTime);
            return datePoint.compareTo(startTime) >= 0 && datePoint.compareTo(endTime) <= 0;
        } catch (ParseException e) {
            LOGGER.error("Invalid time format was found: {}", dateTime);
            return false;
        }
    }

    /**
     * 处理每一行内容，通过内容和框架，把每一行的每一个字段填充到数据容器框架中
     *
     * @param contentArray  数据内容
     * @param dataContainer 数据容器框架
     * @param dataFrame     数据信息框架
     * @param titleOrder    指标序号map
     */
    private void handleContentLine(String[] contentArray,
                                   Map<String, Map<String, List<Map<String, String>>>> dataContainer,
                                   Map<String, List<ReportTest>> dataFrame,
                                   Map<String, Integer> titleOrder) {
        if (contentArray == null) {
            return;
        }
        // 获取该条记录的时间
        String dateTime = getDateTime(contentArray, titleOrder);

        // 获取该条记录时的虚拟用户数
        String visualUserCount = getVisualUserCount(contentArray, titleOrder);

        // 通过遍历数据框架信息，来获取该行数据中的值，放入到数据容器中
        for (Map.Entry<String, List<ReportTest>> metricEntry : dataFrame.entrySet()) {

            // metricName为压测数据指标
            String metricName = metricEntry.getKey();

            // 压测数据指标中，各测试项的明细
            Map<String, List<Map<String, String>>> metricDataMap = dataContainer.get(metricName);
            for (ReportTest reportTest : metricEntry.getValue()) {

                // 测试项名称
                String testName = reportTest.getDesc();
                Map<String, String> timePointData = new HashMap<>();

                // 测试项数据列表
                List<Map<String, String>> metricDataList = metricDataMap.get(testName);
                timePointData.put("time", dateTime);

                // 因为各测试项共用虚拟用户，所以如果是虚拟用户，直接全部放虚拟用户
                if (VUSER_KEY_NAME.equals(metricName)) {
                    timePointData.put("value", visualUserCount);
                    metricDataList.add(timePointData);
                    continue;
                }

                // 如果是total，指标没有后缀，所以单独处理
                if (TOTAL_METRIC_KEY_NAME.equals(testName)) {
                    timePointData.put("value", contentArray[titleOrder.get(metricName)]);
                    metricDataList.add(timePointData);
                    continue;
                }

                // 其他测试项根据逻辑统计处理，即每一个测试指标的后缀加上“-x”，就对第x测试项中的该指标，例如Tps-1，表示第1个测试任务的TPS
                int testOrder = reportTest.getOrder();
                timePointData.put("value", contentArray[titleOrder.get(metricName + "-" + testOrder)]);
                metricDataList.add(timePointData);
            }
        }
    }

    /**
     * 获取一行数据中的时间字段
     *
     * @param contentArray 一行数据分隔之后的数组
     * @param titleOrder   每一个字段名称和数据在数组中的编号
     * @return 时间字段值
     */
    private String getDateTime(String[] contentArray, Map<String, Integer> titleOrder) {
        if (contentArray == null) {
            return "";
        }
        return contentArray[titleOrder.get(DATE_TIME_KEY_NAME)];
    }

    /**
     * 获取一行数据中的虚拟用户字段
     *
     * @param contentArray 一行数据分隔之后的数组
     * @param titleOrder   每一个字段名称和数据在数组中的编号
     * @return 虚拟用户字段值
     */
    private String getVisualUserCount(String[] contentArray, Map<String, Integer> titleOrder) {
        if (contentArray == null) {
            return "";
        }
        return contentArray[titleOrder.get(VUSER_KEY_NAME)];
    }

    /**
     * 通过解析标题列，获取每列数据中，各标题对应的数据序号，便于后续处理每一行数据时，根据map直接拿序号进行查询
     *
     * @param titleLine 标题列
     * @return 每一个标题和所在序号的map
     */
    private Map<String, Integer> getTitleMap(String titleLine) {
        if (StringUtils.isEmpty(titleLine)) {
            return Collections.emptyMap();
        }
        String[] titleArray = titleLine.split(",");
        Map<String, Integer> metrics = new HashMap<>();
        for (int i = 0; i < titleArray.length; i++) {
            if (TEST_DESCRIPTION_KEY_NAME.equals(titleArray[i])) {
                continue;
            }
            metrics.put(titleArray[i], i);
        }
        return metrics;
    }

    /**
     * 根据测试明细和指标明细来定义数据容器框架，该框架封装了查询数据需要使用到信息
     *
     * @param tests 测试信息
     * @return 数据容器框架
     */
    private Map<String, Map<String, List<Map<String, String>>>> getContainerFrame(List<ReportTest> tests) {
        if (tests == null || tests.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, List<Map<String, String>>>> dataFrame = new HashMap<>();
        for (String metric : metrics) {
            Map<String, List<Map<String, String>>> testsData = new HashMap<>();
            for (ReportTest test : tests) {
                List<Map<String, String>> dataList = new ArrayList<>();
                testsData.put(test.getDesc(), dataList);
            }
            dataFrame.put(metric, testsData);
        }
        return dataFrame;
    }

    /**
     * 根据测试明细和指标明细来定义数据信息框架，该框架封装了查询数据需要使用到信息
     *
     * @param tests 测试信息
     * @return 数据信息框架
     */
    private Map<String, List<ReportTest>> getDataFrame(List<ReportTest> tests) {
        if (tests == null || tests.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<ReportTest>> dataFrame = new HashMap<>();
        for (String metric : metrics) {
            dataFrame.put(metric, tests);
        }
        return dataFrame;
    }

    /**
     * 根据第一行title和第二行压测数据内容，分析出一共有几个测试数据，并把测试名称和顺序定义出来，Total也作为一个测试来处理
     *
     * @param titleLine   标题行
     * @param contentLine 内容行
     * @return 文件中封装的测试任务信息
     */
    private List<ReportTest> getTest(String titleLine, String contentLine) {
        if (StringUtils.isEmpty(titleLine)) {
            return Collections.emptyList();
        }
        if (StringUtils.isEmpty(contentLine)) {
            return Collections.emptyList();
        }
        String[] dataArray = contentLine.split(",");
        String[] titleArray = titleLine.split(",");
        List<ReportTest> reportTests = new ArrayList<>();
        int testOrder = 1;
        for (int i = 0; i < titleArray.length; i++) {
            if (!TEST_DESCRIPTION_KEY_NAME.equals(titleArray[i])) {
                // Description对应的列数据，就是一个测试任务的明细，所以不是该列的信息，直接pass
                continue;
            }

            // 发现一个之后，就根据顺序，和内容列中的数据，定义出该测试信息
            reportTests.add(new ReportTest(testOrder, dataArray[i]));
            testOrder++;
        }

        // 默认把Total也作为一个测试处理
        reportTests.add(new ReportTest(0, TOTAL_METRIC_KEY_NAME));
        return reportTests;
    }
}
