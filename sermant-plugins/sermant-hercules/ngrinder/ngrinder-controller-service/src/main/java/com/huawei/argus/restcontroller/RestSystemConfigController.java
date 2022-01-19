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
import org.ngrinder.common.controller.RestAPI;
import org.ngrinder.operation.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.ngrinder.common.util.Preconditions.checkNotEmpty;

/**
 * System configuration controller.
 *
 * @author Alex Qin
 * @since 3.1
 */
@RestController
@RequestMapping("/resr/operation/system_config")
public class RestSystemConfigController extends RestBaseController {

	@Autowired
	private SystemConfigService systemConfigService;

	/**
	 * Open the system configuration editor.[未改变，存在另一个类似方法]
	 *
	 * @return operation/system_config
	 */
	@RequestMapping("")
	public String getOne(ModelMap model) {
		model.addAttribute("content", systemConfigService.getOne());
		return "operation/system_config";
	}


	/**
	 * Save the system configuration.[未改变，存在另一个类似方法]
	 *
	 * @param content system configuration content to be saved
	 * @param model   model
	 * @return operation/system_config
	 */
	@RequestMapping("/save")
	public String save(@RequestParam final String content, ModelMap model) {
		systemConfigService.save(content);
		model.clear();
		return "redirect:/operation/system_config";
	}


	/**
	 * Get the system configuration.
	 *
	 * @return system configuration
	 */
	@RestAPI
	@RequestMapping(value = "/api", method = RequestMethod.GET)
	public HttpEntity<String> getOne() {
		return toJsonHttpEntity(systemConfigService.getOne());
	}

	/**
	 * Save the system configuration.
	 *
	 * @param content system configuration content to be saved
	 * @return true if succeeded
	 */
	@RestAPI
	@RequestMapping(value = "/api", method = RequestMethod.POST)
	public HttpEntity<JSONObject> save(@RequestParam(required = true) final String content) {
		systemConfigService.save(checkNotEmpty(content, "content should be " +
			"passed as parameter"));
		return successJsonHttpEntity();
	}
}

