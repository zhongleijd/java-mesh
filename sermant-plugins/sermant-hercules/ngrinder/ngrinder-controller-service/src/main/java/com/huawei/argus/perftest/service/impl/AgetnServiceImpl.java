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

package com.huawei.argus.perftest.service.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.huawei.argus.perftest.service.IAgentInfoService;
import net.grinder.message.console.AgentControllerState;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.agent.repository.AgentManagerRepository;
import org.ngrinder.agent.service.LocalAgentService;
import org.ngrinder.model.AgentInfo;
import org.ngrinder.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Collections2.filter;

/**
 * @author lWX716491
 * @date 2019/04/24 10:04
 */
@Service
public class AgetnServiceImpl implements IAgentInfoService {
	@Autowired
	protected LocalAgentService cachedLocalAgentService;

	@Autowired
	private AgentManagerRepository agentManagerRepository;

	@Override
	public Collection<AgentInfo> getAgentInfoCollection(User user,final String region) {
		final String userId = user.getUserId();
		Collection<AgentInfo> agents = this.getAllVisible();
		agents = filter(agents, new Predicate<AgentInfo>() {
			@Override
			public boolean apply(AgentInfo agentInfo) {
				return filterAgentByCluster(region, agentInfo.getRegion());
			}
		});
		return agents;
	}
	@Override
	public List<AgentInfo> getAllVisible() {
		List<AgentInfo> agents = Lists.newArrayList();
		for (AgentInfo agentInfo : getAllLocal()) {
			final AgentControllerState state = agentInfo.getState();
			Boolean approved = agentInfo.getApproved();
			if (state != null && state.isActive() && approved) {
				agents.add(agentInfo);
			}
		}
		return agents;
	}

	@Override
	public Collection<AgentInfo> getSelectedAgents(String agentIds) {
		if (agentIds == null || agentIds.length() <= 0)
			return null;
		String[] split = agentIds.split(",");
		List<String> ids = Arrays.asList(split);
		return agentManagerRepository.getSelectedAgents(ids);

	}

	public List<AgentInfo> getAllLocal() {
		return Collections.unmodifiableList(cachedLocalAgentService.getLocalAgents());
	}

	/**
	 * Filter agent list by referring to cluster
	 */
	private boolean filterAgentByCluster(String region, String agentRegion) {
		//noinspection SimplifiableIfStatement
		if (StringUtils.isEmpty(region)) {
			return true;
		} else {
			return agentRegion.startsWith(region + "_owned") || region.equals(agentRegion);
		}
	}
}
