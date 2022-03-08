/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.register.handler;

import com.huawei.register.config.RegisterConfig;
import com.huawei.register.config.RegisterDynamicConfig;
import com.huawei.register.context.RegisterContext;
import com.huawei.register.support.RegisterSwitchSupport;
import com.huawei.sermant.core.common.LoggerFactory;
import com.huawei.sermant.core.plugin.config.PluginConfigManager;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * 关闭注册中心处理
 *
 * @author zhouss
 * @since 2022-01-04
 */
public abstract class SingleStateCloseHandler extends RegisterSwitchSupport {
    /**
     * 注册中心是否关闭
     */
    protected static final AtomicBoolean IS_CLOSED = new AtomicBoolean();

    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 增强对象
     */
    protected Object target;

    /**
     * 方法参数
     */
    protected Object[] arguments;

    public SingleStateCloseHandler() {
        RegisterContext.INSTANCE.registerCloseHandler(this);
    }

    /**
     * 原注册中心状态变更
     *
     * @param allArguments 参数
     * @param obj          增强对象
     * @param originState  变更前的状态
     * @param newState     变更后的状态
     */
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void doChange(Object obj, Object[] allArguments, boolean originState, boolean newState) {
        if (!newState) {
            tryClose();
        }
    }

    /**
     * 关闭注册中心
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void tryClose() {
        if (needCloseRegisterCenter() && IS_CLOSED.compareAndSet(false, true)) {
            try {
                close();
            } catch (Exception ex) {
                // 重置状态
                resetCloseState();
                LOGGER.warning(String.format(Locale.ENGLISH,
                    "Closed register healthy check failed! %s", ex.getMessage()));
            }
        }
    }

    /**
     * 重置开关状态 当某个注册中心关闭失败需要重新关闭时可调用
     */
    private void resetCloseState() {
        IS_CLOSED.set(false);
    }

    /**
     * 子类实现，默认为配置的注册开关 若需修改，则需重新实现该方法 满足条件:
     * <li>已开启spring注册</li>
     * <li>配置中心已下发关闭注册中心指令或者属于单注册的场景</li>
     *
     * @return 是否可关闭注册中心
     */
    protected boolean needCloseRegisterCenter() {
        return (RegisterDynamicConfig.INSTANCE.isNeedCloseOriginRegisterCenter()
            || !PluginConfigManager.getPluginConfig(RegisterConfig.class).isOpenMigration())
            && isEnableSpringRegister();
    }

    /**
     * 关闭注册中心
     *
     * @throws Exception 关闭失败时抛出
     */
    protected abstract void close() throws Exception;

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }
}
