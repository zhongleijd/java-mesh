package org.ngrinder.script.handler;

import org.ngrinder.model.User;
import org.ngrinder.script.model.FileEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 脚本文件及其资源打包分发时的拓展
 * 通过此接口获取额外的资源文件
 *
 * @author y30010171
 * @since 2022-06-25
 **/
public interface ScriptDistributionExtension {
    /**
     * 获取额外的lib和resource文件集合
     *
     * @param handler 脚本处理器
     * @param distDir 需要分发的目录
     * @param testId  当前压测任务ID
     * @param user 脚本用户
     * @param scriptEntry 当前脚本路径
     * @return 文件集合
     * @throws IOException
     */
    List<FileEntry> extensionLibAndResources(ScriptHandler handler, File distDir, Long testId, User user,
        FileEntry scriptEntry) throws IOException;
}
