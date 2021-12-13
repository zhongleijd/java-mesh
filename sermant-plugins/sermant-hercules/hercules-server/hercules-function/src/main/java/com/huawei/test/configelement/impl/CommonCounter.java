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

package com.huawei.test.configelement.impl;

import com.huawei.test.configelement.Counter;
import com.huawei.test.configelement.config.CounterConfig;

/**
 * 功能描述：计数器一般实现
 *
 * @author zl
 * @since 2021-12-09
 */
public class CommonCounter extends Counter {
	@Override
	public void initConfig(CounterConfig config) {

	}

	@Override
	public String nextNumber() {
		return null;
	}

	@Override
	public boolean hasNext() {
		return false;
	}
}
