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

package org.ngrinder.script.service;

import com.beust.jcommander.internal.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.util.EncodingUtils;
import org.ngrinder.infra.config.Config;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.User;
import org.ngrinder.script.handler.ScriptHandler;
import org.ngrinder.script.handler.ScriptHandlerFactory;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 功能描述：
 *
 * @author zl
 * @since 2022-01-17
 */
@Service
public class NfsFileEntryService {
    /**
     * 日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(NfsFileEntryService.class);

    @Autowired
    private Config config;

    @Autowired
    private ScriptHandlerFactory scriptHandlerFactory;

    /**
     * 脚本存放基路径
     */
    private File scriptBaseDir;


    @PostConstruct
    public void init() {
        scriptBaseDir = config.getHome().getScriptDirectory();
    }

    /**
     * 获取指定用户的脚本保存路径，绝对路径
     *
     * @param user 获取用户
     * @return 用户脚本保存的路径
     */
    public File getUserScriptsDir(User user) {
        return config.getHome().getScriptDirectory(user);
    }

    /**
     * 获取指定用户的脚本保存路径，绝对路径
     *
     * @param user 获取用户
     * @param path 用户脚本路径下面的文件子路径
     * @return 用户脚本保存的路径
     */
    public File getUserScriptsDir(User user, String path) {
        return new File(getUserScriptsDir(user), path);
    }

    /**
     * 获取指定测试的所有脚本资源
     *
     * @param filePath 脚本下面某一个路径
     * @return 该测试任务的所有脚本资源
     * @throws IOException io操作异常
     */
    public List<FileEntry> getUserScriptAllFiles(User user, String filePath) throws IOException {
        File oneScriptDir = new File(getUserScriptsDir(user), filePath);
        List<FileEntry> userTestScriptList = new ArrayList<>();
        addFileEntry(oneScriptDir, user, userTestScriptList);
        return userTestScriptList;
    }

    /**
     * 获取指定脚本文件
     *
     * @param perfTest 压测任务明细
     * @return 文件明细
     * @throws IOException 文件不存在
     */
    public FileEntry getSpecifyScript(PerfTest perfTest) throws IOException {
        if (perfTest == null || perfTest.getCreatedUser() == null) {
            return null;
        }
        User createdUser = perfTest.getCreatedUser();
        File testScriptBaseDir = getUserScriptsDir(createdUser);
        File testScript = new File(testScriptBaseDir, perfTest.getScriptName());
        return buildFileEntry(testScript, createdUser);
    }

    /**
     * 获取指定脚本文件
     *
     * @param perfTest 压测任务明细
     * @param user     用户
     * @return 文件明细
     * @throws IOException 文件不存在
     */
    public FileEntry getSpecifyScript(User user, PerfTest perfTest) throws IOException {
        if (perfTest == null || user == null) {
            return null;
        }
        File testScript = new File(getUserScriptsDir(user), perfTest.getScriptName());
        return buildFileEntry(testScript, user);
    }

    /**
     * 获取指定脚本文件
     *
     * @param user 用户
     * @param file 需要获取的文件在路径
     * @return 文件明细
     * @throws IOException 文件不存在
     */
    public FileEntry getSpecifyScript(User user, File file) throws IOException {
        if (file == null || user == null) {
            return null;
        }
        File testScript = new File(getUserScriptsDir(user), file.getPath());
        return buildFileEntry(testScript, user);
    }

    /**
     * 获取指定脚本文件
     *
     * @param user     用户
     * @param filePath 用户脚本子路径需要获取的文件在路径
     * @return 文件明细
     * @throws IOException 文件不存在
     */
    public FileEntry getSpecifyScript(User user, String filePath) throws IOException {
        if (StringUtils.isEmpty(filePath) || user == null) {
            return null;
        }
        File testScript = new File(getUserScriptsDir(user), filePath);
        return buildFileEntry(testScript, user);
    }

    /**
     * 把指定文件封装成{@link FileEntry}放入到列表中
     *
     * @param file               如果是文件夹，则递归调用该方法，如果是文件则直接封装
     * @param user               用户
     * @param userTestScriptList 保存明细的列表
     */
    private void addFileEntry(File file, User user, List<FileEntry> userTestScriptList) throws IOException {
        if (file == null || user == null) {
            return;
        }
        if (file.isFile()) {
            FileEntry fileEntry = buildFileEntry(file, user);
            userTestScriptList.add(fileEntry);
            return;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File childFile : files) {
            addFileEntry(childFile, user, userTestScriptList);
        }
    }

    /**
     * 封装文件到{@link FileEntry}中返回
     *
     * @param file 文件
     * @return FileEntry对象
     */
    private FileEntry buildFileEntry(File file, User user) throws IOException {
        if (file == null) {
            throw new IOException("The file not exist.");
        }
        if (!file.exists()) {
            throw new IOException("The file not exist:" + file);

        }
        if (file.isDirectory()) {
            throw new IOException("The file is directory:" + file);
        }
        FileEntry script = new FileEntry();
        script.setPath(file.getPath().replace(getUserScriptsDir(user).getPath(), ""));
        script.setFileType(FileType.getFileTypeByExtension(FilenameUtils.getExtension(file.getName())));
        byte[] contentBytes;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream cacheByteArray = new ByteArrayOutputStream()) {
            byte[] readCache = new byte[4096];
            int readLength = 0;
            while ((readLength = fis.read(readCache)) != -1) {
                cacheByteArray.write(readCache, 0, readLength);
            }
            cacheByteArray.flush();
            contentBytes = cacheByteArray.toByteArray();
        }
        if (script.getFileType().isEditable()) {
            String autoDetectedEncoding = EncodingUtils.detectEncoding(contentBytes, "UTF-8");
            script.setContent((new String(contentBytes, autoDetectedEncoding)));
            script.setEncoding(autoDetectedEncoding);
        }
        script.setCreatedUser(user);
        script.setContentBytes(contentBytes);
        script.setDescription("");
        script.setRevision(1);
        script.setLastRevision(1);
        script.setCreatedDate(new Date(file.lastModified()));
        script.setLastModifiedDate(new Date(file.lastModified()));
        script.setFileSize(contentBytes.length);
        return script;
    }

    /**
     * 复制文件到指定目录里面
     *
     * @param fileEntry 文件信息
     * @param toDir     目标目录
     * @throws IOException 文件流操作异常
     */
    public void writeContentTo(FileEntry fileEntry, File toDir) throws IOException {
        if (fileEntry == null || toDir == null) {
            return;
        }
        String filePath = fileEntry.getPath();
        String fileName = new File(filePath).getName();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileEntry.getContentBytes())) {
            FileUtils.copyInputStreamToFile(inputStream, new File(toDir, fileName));
        }
    }

    /**
     * 保存用户脚本文件到用户脚本目录
     *
     * @param user      脚本上传用户
     * @param fileEntry 脚本内容明细
     * @throws IOException io异常
     */
    public void saveFile(User user, FileEntry fileEntry) throws IOException {
        if (user == null || fileEntry == null) {
            return;
        }
        if (fileEntry.getFileType() == FileType.DIR) {
            File needCreateDir = new File(getUserScriptsDir(user), fileEntry.getPath());
            Files.createDirectories(Paths.get(needCreateDir.getPath()));
        } else {
            File needCreateFile = new File(getUserScriptsDir(user), fileEntry.getPath());
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileEntry.getContentBytes())) {
                FileUtils.copyInputStreamToFile(inputStream, needCreateFile);
            }
        }
    }

    /**
     * 创建用户脚本路径
     *
     * @param user 需要创建脚本路径的用户
     */
    public void prepareUserScriptBaseDir(User user) {
        if (user == null) {
            return;
        }
        File userScriptDir = new File(scriptBaseDir, user.getUserId());
        boolean makeDirResult = userScriptDir.mkdirs();
        LOG.info("Create file result, {}:{}", userScriptDir.getPath(), makeDirResult);
    }

    public void addFolder(User user, String path, String child) throws IOException {
        if (user == null) {
            return;
        }
        Files.createDirectories(Paths.get(getUserScriptsDir(user).getPath(), path, child));
    }

    /**
     * Get the appropriate {@link ScriptHandler} subclass for the given
     * {@link FileEntry}.
     *
     * @param scriptEntry script entry
     * @return scriptHandler
     */
    public ScriptHandler getScriptHandler(FileEntry scriptEntry) {
        return scriptHandlerFactory.getHandler(scriptEntry);
    }

    /**
     * Get the appropriate {@link ScriptHandler} subclass for the given
     * ScriptHandler key.
     *
     * @param key script entry
     * @return scriptHandler
     */
    public ScriptHandler getScriptHandler(String key) {
        return scriptHandlerFactory.getHandler(key);
    }

    /**
     * Check file existence.
     *
     * @param user user
     * @param path path in user repo
     * @return true if exists.
     */
    public boolean hasFileEntry(User user, String path) throws IOException {
        return getSpecifyScript(user, path) != null;
    }

    /**
     * 删除指定用户脚本管理路径下面的文件
     *
     * @param user      用户
     * @param path      文件路径
     * @param fileNames 文件绝对路径列表
     * @throws IOException io异常
     */
    public void deleteFile(User user, String path, String[] fileNames) throws IOException {
        if (user == null) {
            LOG.error("Can not find user for privilege.");
            return;
        }
        List<File> files = new ArrayList<>();
        for (String fileName : fileNames) {
            File userScriptsDir = getUserScriptsDir(user, path);
            files.add(new File(userScriptsDir, fileName));
        }
        deleteFile(files);
    }

    /**
     * 删除指定用户脚本管理路径下面的文件
     *
     * @param files 文件绝对路径列表
     * @throws IOException io异常
     */
    private void deleteFile(List<File> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (File file : files) {
            if (!file.exists()) {
                continue;
            }
            if (file.isFile()) {
                Files.deleteIfExists(Paths.get(file.getPath()));
                continue;
            }
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                Files.deleteIfExists(Paths.get(file.getPath()));
                continue;
            }
            deleteFile(Lists.newArrayList(childFiles));
            Files.deleteIfExists(Paths.get(file.getPath()));
        }
    }

    /**
     * 删除指定用户指定路径的文件，如果是文件夹，就递归删除
     *
     * @param user 需要删除文件的用户
     * @param path 需要删除的文件路径
     * @throws IOException io异常
     */
    public void deleteFile(User user, String path) throws IOException {
        if (user == null || StringUtils.isEmpty(path)) {
            return;
        }
        File addPathFile = new File(getUserScriptsDir(user), path);
        if (!addPathFile.exists()) {
            return;
        }
        if (addPathFile.isFile()) {
            Files.deleteIfExists(Paths.get(addPathFile.getPath()));
        }
        File[] childFiles = addPathFile.listFiles();
        if (childFiles == null || childFiles.length == 0) {
            Files.deleteIfExists(Paths.get(addPathFile.getPath()));
            return;
        }
        deleteFile(Lists.newArrayList(childFiles));
        Files.deleteIfExists(Paths.get(addPathFile.getPath()));
    }
}
