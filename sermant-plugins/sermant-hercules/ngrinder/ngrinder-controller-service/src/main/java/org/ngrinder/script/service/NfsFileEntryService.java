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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.util.EncodingUtils;
import org.ngrinder.common.util.PathUtils;
import org.ngrinder.common.util.UrlUtils;
import org.ngrinder.infra.config.Config;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.User;
import org.ngrinder.script.handler.ProjectHandler;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.ngrinder.common.util.CollectionUtils.buildMap;
import static org.ngrinder.common.util.CollectionUtils.newHashMap;

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
     * 获取指定用户的脚本保存路径
     *
     * @return 用户脚本保存的路径
     */
    public File getBaseScriptsDir() {
        return config.getHome().getScriptDirectory();
    }

    /**
     * 获取指定用户的脚本保存路径
     *
     * @param user 获取用户
     * @return 用户脚本保存的路径
     */
    public File getUserScriptsDir(User user) {
        return config.getHome().getScriptDirectory(user);
    }

    /**
     * 获取指定用户的脚本保存路径
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
        addFileEntry(oneScriptDir, userTestScriptList);
        for (FileEntry fileEntry : userTestScriptList) {
            fileEntry.setCreatedUser(user);
            fileEntry.setLastModifiedUser(user);
        }
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
        if (perfTest == null) {
            return null;
        }
        File testScriptBaseDir = getUserScriptsDir(perfTest.getCreatedUser());
        File testScript = new File(testScriptBaseDir, perfTest.getScriptNameInShort());
        return createFileTypeEntry(testScript);
    }

    /**
     * 获取指定脚本文件
     *
     * @param file 需要获取的文件在路径
     * @return 文件明细
     * @throws IOException 文件不存在
     */
    public FileEntry getSpecifyScript(File file) throws IOException {
        if (file == null) {
            return null;
        }
        return createFileTypeEntry(file);
    }

    /**
     * 获取指定脚本文件
     *
     * @param filePath 需要获取的文件在路径
     * @return 文件明细
     * @throws IOException 文件不存在
     */
    public FileEntry getSpecifyScript(String filePath) throws IOException {
        if (StringUtils.isEmpty(filePath)) {
            return null;
        }
        return createFileTypeEntry(new File(filePath));
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
        File testScript = new File(getUserScriptsDir(user), perfTest.getScriptNameInShort());
        return createFileTypeEntry(testScript);
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
        return createFileTypeEntry(testScript);
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
        return createFileTypeEntry(testScript);
    }

    /**
     * 把指定文件封装成{@link FileEntry}放入到列表中
     *
     * @param file               如果是文件夹，则递归调用该方法，如果是文件则直接封装
     * @param userTestScriptList 保存明细的列表
     */
    private void addFileEntry(File file, List<FileEntry> userTestScriptList) throws IOException {
        if (file == null) {
            return;
        }
        if (file.isFile()) {
            FileEntry fileEntry = createFileTypeEntry(file);
            userTestScriptList.add(fileEntry);
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File childFile : files) {
                addFileEntry(childFile, userTestScriptList);
            }
        }
    }

    /**
     * 封装文件到{@link FileEntry}中返回
     *
     * @param file 文件
     * @return FileEntry对象
     */
    private FileEntry createFileTypeEntry(File file) throws IOException {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        FileEntry script = new FileEntry();
        script.setPath(file.getPath());
        script.setFileType(FileType.getFileTypeByExtension(FilenameUtils.getExtension(script.getFileName())));
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
            script.setContent((new String(contentBytes, autoDetectedEncoding)).replaceAll("&quot;", "\""));
            script.setEncoding(autoDetectedEncoding);
        }
        script.setContentBytes(contentBytes);
        script.setDescription(file.getCanonicalPath());
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
     * @param each  文件信息
     * @param toDir 目标目录
     * @throws IOException 文件流操作异常
     */
    public void writeContentTo(FileEntry each, File toDir) throws IOException {
        if (each == null || toDir == null) {
            return;
        }
        String filePath = each.getPath();
        String fileName = new File(filePath).getName();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(each.getContentBytes())) {
            FileUtils.copyInputStreamToFile(inputStream, new File(toDir, fileName));
        }
    }

    public void saveFile(User user, FileEntry fileEntry) throws IOException {
        if (user == null || fileEntry == null) {
            return;
        }
        if (fileEntry.getFileType() == FileType.DIR) {
            File needCreateDir = new File(getUserScriptsDir(user), fileEntry.getPath());
            boolean makeDirResult = needCreateDir.mkdirs();
            LOG.info("Create file result, {}:{}", needCreateDir.getPath(), makeDirResult);
        } else {
            File thisScriptBaseDir = new File(scriptBaseDir, user.getUserId());
            File needCreateFile = new File(thisScriptBaseDir, fileEntry.getPath());
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileEntry.getContentBytes())) {
                FileUtils.copyInputStreamToFile(inputStream, needCreateFile);
            }
        }
    }

    public void prepare(User user) {
        if (user == null) {
            return;
        }
        File userScriptDir = new File(scriptBaseDir, user.getUserId());
        boolean makeDirResult = userScriptDir.mkdirs();
        LOG.info("Create file result, {}:{}", userScriptDir.getPath(), makeDirResult);
    }

    public void addFolder(User user, String path, String child) {
        if (user == null) {
            return;
        }
        File addPathDir = new File(getUserScriptsDir(user), path);
        File addFoldDir = new File(addPathDir, child);
        boolean makeDirResult = addFoldDir.mkdirs();
        LOG.info("Create file result, {}:{}", addFoldDir.getPath(), makeDirResult);
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

    public void deleteFile(User user, String path, String[] files) {
        if (user == null || files == null) {
            return;
        }
        for (String file : files) {
            deleteFile(user, path, file);
        }
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

    public void deleteFile(User user, String path, String file) {
        if (user == null || StringUtils.isEmpty(file)) {
            return;
        }
        File userScriptDir = new File(scriptBaseDir, user.getUserId());
        File addPathDir = new File(userScriptDir, path);
        File addFoldDir = new File(addPathDir, file);
        boolean makeDirResult = addFoldDir.delete();
        LOG.info("Delete file result, {}:{}", addFoldDir.getPath(), makeDirResult);
    }

    public void deleteFile(User user, String path) {
        if (user == null || StringUtils.isEmpty(path)) {
            return;
        }
        File userScriptDir = new File(scriptBaseDir, user.getUserId());
        File addPathDir = new File(userScriptDir, path);
        File[] files = addPathDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            deleteFile(user, path, file.getPath());
        }
    }

    /**
     * Create new FileEntry.
     *
     * @param user           user
     * @param path           base path path
     * @param fileName       fileName
     * @param name           name
     * @param url            url
     * @param scriptHandler  script handler
     * @param libAndResource true if lib and resources should be created
     * @return created file entry. main test file if it's the project creation.
     */
    public FileEntry prepareNewEntry(User user, String path, String fileName, String name, String url,
                                     ScriptHandler scriptHandler, boolean libAndResource, String options) {
        if (scriptHandler instanceof ProjectHandler) {
            String scriptContent = loadTemplate(user, getScriptHandler("groovy"), url, name, options);
            scriptHandler.prepareScriptEnv(user, path, fileName, name, url, libAndResource, scriptContent);
            return null;
        }
        path = PathUtils.join(path, fileName);
        FileEntry fileEntry = new FileEntry();
        fileEntry.setPath(path);
        fileEntry.setContent(loadTemplate(user, scriptHandler, url, name, options));
        fileEntry.setProperties(buildMap("targetHosts", UrlUtils.getHost(url)));
        return fileEntry;
    }

    /**
     * Load freemarker template for quick test.
     *
     * @param user    user
     * @param handler handler
     * @param url     url
     * @param name    name
     * @return generated test script
     */
    public String loadTemplate(User user, ScriptHandler handler, String url, String name,
                               String options) {
        Map<String, Object> map = newHashMap();
        map.put("url", url);
        map.put("userName", user.getUserName());
        map.put("name", name);
        map.put("options", options);
        return handler.getScriptTemplate(map);
    }

    private void throwException() throws IOException {
        throw new IOException("File control failed.");
    }
}
