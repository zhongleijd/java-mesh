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

package com.huawei.test.configelement.config;

/**
 * 功能描述：计数器配置
 *
 * @author zl
 * @since 2021-12-09
 */
public class CounterConfig {
	/**
	 * 数字开始值
	 */
	private final int startValue;

	/**
	 * 数字增长幅度
	 */
	private final int increment;

	/**
	 * 最大值
	 */
	private final int maxValue;

	/**
	 * 数字格式
	 */
	private final String numberFormat;

	/**
	 * 变量名称
	 */
	private final String variableName;

	/**
	 * 是否每一个线程独立使用一个计数器
	 */
	private final boolean trackEachUser;

	/**
	 * 是否每次迭代完成之后都重新开始计数
	 */
	private final boolean resetEachIteration;

	public int getStartValue() {
		return startValue;
	}

	public int getIncrement() {
		return increment;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public String getNumberFormat() {
		return numberFormat;
	}

	public String getVariableName() {
		return variableName;
	}

	public boolean isTrackEachUser() {
		return trackEachUser;
	}

	public boolean isResetEachIteration() {
		return resetEachIteration;
	}

	private CounterConfig(Builder builder) {
		this.startValue = builder.startValue;
		this.increment = builder.increment;
		this.maxValue = builder.maxValue;
		this.numberFormat = builder.numberFormat;
		this.variableName = builder.variableName;
		this.trackEachUser = builder.trackEachUser;
		this.resetEachIteration = builder.resetEachIteration;
	}

	public static class Builder {
		/**
		 * 数字开始值
		 */
		private int startValue;

		/**
		 * 数字增长幅度
		 */
		private int increment;

		/**
		 * 最大值
		 */
		private int maxValue;

		/**
		 * 数字格式
		 */
		private String numberFormat;

		/**
		 * 变量名称
		 */
		private String variableName;

		/**
		 * 是否每一个线程独立使用一个计数器
		 */
		private boolean trackEachUser;

		/**
		 * 是否每次迭代完成之后都重新开始计数
		 */
		private boolean resetEachIteration;

		public Builder setStartValue(int startValue) {
			this.startValue = startValue;
			return this;
		}

		public Builder setIncrement(int increment) {
			this.increment = increment;
			return this;
		}

		public Builder setMaxValue(int maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder setNumberFormat(String numberFormat) {
			this.numberFormat = numberFormat;
			return this;
		}

		public Builder setVariableName(String variableName) {
			this.variableName = variableName;
			return this;
		}

		public Builder setTrackEachUser(boolean trackEachUser) {
			this.trackEachUser = trackEachUser;
			return this;
		}

		public Builder setResetEachIteration(boolean resetEachIteration) {
			this.resetEachIteration = resetEachIteration;
			return this;
		}

		public CounterConfig build() {
			return new CounterConfig(this);
		}
	}
}
