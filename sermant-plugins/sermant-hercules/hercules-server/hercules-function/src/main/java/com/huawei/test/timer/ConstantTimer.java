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

package com.huawei.test.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 功能描述：线程停止指定时长
 *
 * @author zl
 * @since 2021-12-09
 */
public class ConstantTimer {
	/**
	 * 日志
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ConstantTimer.class);

	/**
	 * 线程停止指定时长
	 *
	 * @param time     停止的时长
	 * @param timeUnit 停止时长的单位
	 */
	public static void delay(long time, TimeUnit timeUnit) {
		if (timeUnit == null || time <= 0) {
			return;
		}
		try {
			timeUnit.sleep(time);
		} catch (InterruptedException e) {
			LOGGER.error("Sleep current thread fail:{}", e.getMessage());
		}
	}
}
