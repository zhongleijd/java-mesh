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
package org.ngrinder.home.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.constant.ControllerConstants;
import org.ngrinder.common.controller.BaseController;
import org.ngrinder.common.util.ThreadUtils;
import org.ngrinder.region.service.RegionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.ngrinder.common.util.Preconditions.checkNotNull;

/**
 * Home index page controller.
 *
 * @author JunHo Yoon
 * @since 3.0
 */
@Controller
public class HomeController extends BaseController implements ControllerConstants {

	private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

	@Autowired
	private RegionService regionService;

	private static Gson rawObjectJsonSerializer = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Initialize {@link HomeController}.
	 */
	@PostConstruct
	public void init() {
	}

	/**
	 * Return the health check message. If there is shutdown lock, it returns
	 * 503. Otherwise it returns region lists.
	 *
	 * @param response response
	 * @return region list
	 */
	@RequestMapping("/check/healthcheck")
	public HttpEntity<String> healthCheck(HttpServletResponse response) {
		if (getConfig().hasShutdownLock()) {
			try {
				response.sendError(503, "nGrinder is about to down");
			} catch (IOException e) {
				LOG.error("While running healthCheck() in HomeController, the error occurs.");
				LOG.error("Details : ", e);
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("current", regionService.getCurrent());
		map.put("regions", regionService.getAll());
		return toJsonHttpEntity(map, rawObjectJsonSerializer);
	}

	/**
	 * Return health check message with 1 sec delay. If there is shutdown lock,
	 * it returns 503. Otherwise, it returns region lists.
	 *
	 * @param sleep    in milliseconds.
	 * @param response response
	 * @return region list
	 */
	@ResponseBody
	@RequestMapping("/check/healthcheck_slow")
	public HttpEntity<String> healthCheckSlowly(@RequestParam(value = "delay", defaultValue = "1000") int sleep,
												HttpServletResponse response) {
		ThreadUtils.sleep(sleep);
		return healthCheck(response);
	}

	private void setLanguage(String lan, HttpServletResponse response, HttpServletRequest request) {
		LocaleResolver localeResolver = checkNotNull(RequestContextUtils.getLocaleResolver(request),
				"No LocaleResolver found!");
		LocaleEditor localeEditor = new LocaleEditor();
		String language = StringUtils.defaultIfBlank(lan,
				getConfig().getControllerProperties().getProperty(PROP_CONTROLLER_DEFAULT_LANG));
		localeEditor.setAsText(language);
		localeResolver.setLocale(request, response, (Locale) localeEditor.getValue());
	}
}
