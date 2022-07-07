package com.huawei.test;

/**
 * 共享任务调用，用于主任务调用子任务的测试方法
 *
 * @author y30010171
 * @since 2022-06-29
 **/
public interface ShareTest {
    /**
     * 调用自身测试方法
     */
    void invokeTest();
}
