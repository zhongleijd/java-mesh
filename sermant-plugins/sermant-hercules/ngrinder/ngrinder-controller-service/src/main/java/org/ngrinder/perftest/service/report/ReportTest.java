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

/**
 * 功能描述：
 *
 * @author zl
 * @since 2022-03-23
 */
public class ReportTest {
    private final int order;
    private final String desc;

    public ReportTest(int order, String desc) {
        this.order = order;
        this.desc = desc;
    }

    public int getOrder() {
        return order;
    }

    public String getDesc() {
        return desc;
    }
}
