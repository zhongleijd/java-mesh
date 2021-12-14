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

package com.huawei.test.asserts;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * 功能描述：正则表达式校验器
 *
 * @author zl
 * @since 2021-12-09
 */
public class RegularAssert {
	/**
	 * 日志
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(RegularAssert.class);

	/**
	 * 判断给定的内容是否与正则表达式匹配，使用java.util.regex.Pattern实现
	 *
	 * @param content           内容
	 * @param regularExpression 正则表达式
	 * @return true：内容匹配，false:内容不匹配
	 */
	public static boolean assertRegular(String content, String regularExpression) {
		if (StringUtils.isEmpty(content)) {
			LOGGER.error("The content used for regular expression matching is an empty string.");
			return false;
		}
		if (StringUtils.isEmpty(regularExpression)) {
			LOGGER.error("The regular expression used for content matching is empty.");
			return false;
		}
		return Pattern.matches(regularExpression, content);
	}
}
