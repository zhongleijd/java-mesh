/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.flowcontrol.retry;

import com.huawei.flowcontrol.common.entity.DubboRequestEntity;
import com.huawei.flowcontrol.common.handler.retry.AbstractRetry;
import com.huawei.flowcontrol.common.handler.retry.Retry;
import com.huawei.flowcontrol.common.handler.retry.RetryContext;
import com.huawei.flowcontrol.common.util.ConvertUtils;
import com.huawei.flowcontrol.service.InterceptorSupporter;
import com.huawei.sermant.core.common.LoggerFactory;
import com.huawei.sermant.core.plugin.agent.entity.ExecuteContext;

import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * apache dubbo拦截后的增强类,埋点定义sentinel资源
 *
 * @author zhouss
 * @since 2022-02-11
 */
public class ApacheDubboInvokerInterceptor extends InterceptorSupporter {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final Retry retry = new ApacheDubboRetry();

    /**
     * 转换apache dubbo 注意，该方法不可抽出，由于宿主依赖仅可由该拦截器加载，因此抽出会导致找不到类
     *
     * @param invocation 调用信息
     * @return DubboRequestEntity
     */
    private DubboRequestEntity convertToApacheDubboEntity(Invocation invocation) {
        // 高版本使用api invocation.getTargetServiceUniqueName获取路径，此处使用版本加接口，达到的最终结果一致
        String apiPath = ConvertUtils.buildApiPath(invocation.getInvoker().getInterface().getName(),
            invocation.getAttachment(ConvertUtils.DUBBO_ATTACHMENT_VERSION), invocation.getMethodName());
        return new DubboRequestEntity(apiPath, invocation.getAttachments());
    }

    /**
     * 此处重复代码与{@link com.huawei.flowcontrol.retry.AlibabaDubboInvokerInterceptor}相同之处
     * <H2>不可抽出</H2>
     * 由于两个框架类权限定名不同, 且仅当当前的拦截器才可加载宿主类
     *
     * @param obj          增强对象
     * @param allArguments 方法参数
     * @param ret          响应结果
     * @param isNeedThrow  是否需抛出异常
     * @return 方法调用器
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Object invokeRetryMethod(Object obj, Object[] allArguments, Object ret, boolean isNeedThrow,
        boolean isRetry) throws Throwable {
        try {
            if (obj instanceof AbstractClusterInvoker) {
                final Invocation invocation = (Invocation) allArguments[0];
                final List<Invoker<?>> invokers = (List<Invoker<?>>) allArguments[1];
                final Method checkInvokers = getMethodCheckInvokers();
                final Method select = getMethodSelect();
                if (checkInvokers == null || select == null) {
                    LOGGER.warning(String.format(Locale.ENGLISH, "It does not support retry for class %s",
                        obj.getClass().getCanonicalName()));
                    return ret;
                }
                if (isRetry) {
                    invocation.getAttachments().put(RETRY_KEY, RETRY_VALUE);
                }

                // 校验invokers
                checkInvokers.invoke(obj, invokers, invocation);
                LoadBalance loadBalance = (LoadBalance) allArguments[2];

                // 选择invoker
                final Invoker<?> invoke = (Invoker<?>) select.invoke(obj, loadBalance, invocation, invokers, null);

                // 执行调用
                final Result result = invoke.invoke(invocation);
                if (result.hasException() && isNeedThrow) {
                    throw result.getException();
                }
                return result;
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            LOGGER.warning("No such Method ! " + ex.getMessage());
        }
        return ret;
    }

    private Method getMethodSelect() {
        return getInvokerMethod("select", func -> {
            try {
                final Method method = AbstractClusterInvoker.class
                    .getDeclaredMethod("select", LoadBalance.class, Invocation.class, List.class, List.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ex) {
                LOGGER.warning("No such Method select! " + ex.getMessage());
            }
            return null;
        });
    }

    private Method getMethodCheckInvokers() {
        return getInvokerMethod("checkInvokers", func -> {
            try {
                final Method method = AbstractClusterInvoker.class
                    .getDeclaredMethod("checkInvokers", List.class, Invocation.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ex) {
                LOGGER.warning("No such Method checkInvokers! " + ex.getMessage());
            }
            return null;
        });
    }

    @Override
    protected final ExecuteContext doBefore(ExecuteContext context) {
        context.skip(null);
        return context;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    protected final ExecuteContext doAfter(ExecuteContext context) {
        Object result = context.getResult();
        final Object[] allArguments = context.getArguments();
        final Invocation invocation = (Invocation) allArguments[0];
        try {
            // 调用宿主方法
            RetryContext.INSTANCE.markRetry(retry);
            result = invokeRetryMethod(context.getObject(), allArguments, result, false, false);
            final List<io.github.resilience4j.retry.Retry> handlers = retryHandler
                .getHandlers(convertToApacheDubboEntity(invocation));
            if (!handlers.isEmpty() && needRetry(handlers.get(0), result, ((AsyncRpcResult) result).getException())) {
                RetryContext.INSTANCE.markRetry(retry);
                result = handlers.get(0)
                    .executeCheckedSupplier(() -> invokeRetryMethod(context.getObject(), allArguments,
                        context.getResult(), true, true));
                invocation.getAttachments().remove(RETRY_KEY);
            }
        } catch (Throwable throwable) {
            result = AsyncRpcResult.newDefaultAsyncResult(throwable, invocation);
        } finally {
            RetryContext.INSTANCE.removeRetry();
        }
        context.changeResult(result);
        return context;
    }

    public static class ApacheDubboRetry extends AbstractRetry {
        @Override
        public boolean needRetry(Set<String> statusList, Object result) {
            // dubbo不支持状态码
            return false;
        }

        @Override
        public Class<? extends Throwable>[] retryExceptions() {
            return getRetryExceptions();
        }

        @Override
        public RetryFramework retryType() {
            return RetryFramework.APACHE_DUBBO;
        }
    }
}
