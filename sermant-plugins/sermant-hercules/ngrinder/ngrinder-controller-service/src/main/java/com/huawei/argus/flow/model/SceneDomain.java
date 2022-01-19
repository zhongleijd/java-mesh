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

package com.huawei.argus.flow.model;

import org.ngrinder.model.PerfScene;
import org.ngrinder.model.PerfSceneApi;

import java.util.List;

/**
 * Created by x00377290 on 2019/4/19.
 */
public class SceneDomain {
    private PerfScene scene;
	private List<PerfSceneApi> apis;

	public List<PerfSceneApi> getApis() {
		return apis;
	}
	public void setApis(List<PerfSceneApi> apis) {
		this.apis = apis;
	}


    public PerfScene getScene() {
        return scene;
    }

    public void setScene(PerfScene scene) {
        this.scene = scene;
    }


}
