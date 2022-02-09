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

package com.huawei.argus.serializer;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.engine.common.EngineException;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.agent.ClearCacheMessage;
import net.grinder.messages.agent.DistributeFileMessage;
import net.grinder.messages.agent.DistributionCacheCheckpointMessage;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;
import net.grinder.util.StreamCopier;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 功能描述：自定义FileStore, 替换原有的grinder的FileStore，重构脚本文件的操作逻辑
 *
 * @author zl
 * @since 2022-01-26
 */
public class CustomFileStore {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFileStore.class);

    /**
     * readme文件
     */
    private final File readmeFile;

    /**
     * 与controller通信拉去下来的文件所在文件夹
     */
    private final Directory incomingDirectory;

    /**
     * 从incomingDirectory处理之后的文件，当前需要执行的脚本文件和资源存放的位置
     */
    private final Directory currentDirectory;

    /**
     * 为true时，文件夹下面创建文件，不删除原文件，反之删除
     */
    private boolean incremental;

    /**
     * 缓存最大值
     */
    private volatile CacheHighWaterMark cacheHighWaterMark = new CustomFileStore.OutOfDateCacheHighWaterMark();

    /**
     * 根据agent主目录初始化该fileStore的各个子路径
     *
     * @param directory agent主目录所在文件夹
     * @throws CustomFileStore.FileStoreException 文件操作异常
     */
    public CustomFileStore(File directory) throws CustomFileStore.FileStoreException {
        File rootDirectory = directory.getAbsoluteFile();
        if (rootDirectory.exists()) {
            if (!rootDirectory.isDirectory()) {
                throw new CustomFileStore.FileStoreException("Could not write to directory '" + rootDirectory + "' as file with that name already exists");
            }

            if (!rootDirectory.canWrite()) {
                throw new CustomFileStore.FileStoreException("Could not write to directory '" + rootDirectory + "'");
            }
        }

        this.readmeFile = new File(rootDirectory, "README.txt");

        try {
            this.incomingDirectory = new Directory(new File(rootDirectory, "incoming"));
            this.currentDirectory = new Directory(new File(rootDirectory, "current"));
        } catch (Directory.DirectoryException exception) {
            throw new CustomFileStore.FileStoreException(exception.getMessage(), exception);
        }

        this.incremental = false;
    }

    /**
     * 获取处理好的脚本文件和资源文件
     *
     * @return 处理好的脚本文件和资源文件
     * @throws CustomFileStore.FileStoreException 文件操作异常
     */
    public Directory getDirectory() throws CustomFileStore.FileStoreException {
        try {
            synchronized (incomingDirectory) {
                if (incomingDirectory.getFile().exists()) {
                    incomingDirectory.copyTo(currentDirectory, incremental);
                }
                incremental = true;
            }

            return currentDirectory;
        } catch (IOException exception) {
            UncheckedInterruptedException.ioException(exception);
            throw new CustomFileStore.FileStoreException("Could not create file store directory", exception);
        }
    }

    /**
     * 获取缓存有效标识
     *
     * @return 缓存有效标识
     */
    public CacheHighWaterMark getCacheHighWaterMark() {
        return this.cacheHighWaterMark;
    }

    /**
     * 注册controller发送过来的消息处理器
     *
     * @param messageDispatcher controller发送过来的消息路由
     */
    public void registerMessageHandlers(MessageDispatchRegistry messageDispatcher) {
        messageDispatcher.set(ClearCacheMessage.class, new MessageDispatchRegistry.AbstractHandler<ClearCacheMessage>() {
            public void handle(ClearCacheMessage message) throws CommunicationException {
                LOGGER.info("Clearing file store");

                try {
                    synchronized (incomingDirectory) {
                        incomingDirectory.deleteContents();
                        incremental = false;
                    }
                } catch (Directory.DirectoryException exception) {
                    LOGGER.error(exception.getMessage());
                    throw new CommunicationException(exception.getMessage(), exception);
                }
            }
        });
        messageDispatcher.set(DistributeFileMessage.class, new MessageDispatchRegistry.AbstractHandler<DistributeFileMessage>() {
            public void handle(DistributeFileMessage message) throws CommunicationException {
                try {
                    synchronized (incomingDirectory) {
                        incomingDirectory.create();
                        createReadmeFile();
                        FileContents fileContents = message.getFileContents();
                        if (fileContents == null) {
                            LOGGER.info("The content of distribute file is null.");
                            return;
                        }
                        LOGGER.info("Updating file store: {}", fileContents);
                        fileContents.create(incomingDirectory);

                        // 解压传过来的压缩包
                        File zipFile = incomingDirectory.getFile(new File("dist.zip"));
                        if (!zipFile.exists()) {
                            return;
                        }
                        String rootDir = incomingDirectory.getFile().getParent();
                        ZipFileUtil.unzipFile(zipFile, rootDir);
                        File unzipDistFile = Paths.get(rootDir, "dist").toFile();
                        Directory unzipDistDir = new Directory(unzipDistFile);
                        unzipDistDir.copyTo(incomingDirectory, false);
                        ZipFileUtil.deleteChildFile(unzipDistFile);
                    }
                } catch (FileContents.FileContentsException | IOException exception) {
                    LOGGER.error("Occur an error when handle distribute file:{}", exception.getMessage());
                    throw new CommunicationException(exception.getMessage(), exception);
                }
            }
        });
        messageDispatcher.set(DistributionCacheCheckpointMessage.class, new MessageDispatchRegistry.AbstractHandler<DistributionCacheCheckpointMessage>() {
            public void handle(DistributionCacheCheckpointMessage message) {
                cacheHighWaterMark = message.getCacheHighWaterMark();
            }
        });
    }

    private void createReadmeFile() throws CommunicationException {
        if (!this.readmeFile.exists()) {
            try {
                (new StreamCopier(4096, true)).copy(this.getClass().getResourceAsStream("resources/FileStoreReadme.txt"), new FileOutputStream(this.readmeFile));
            } catch (IOException exception) {
                UncheckedInterruptedException.ioException(exception);
                LOGGER.error(exception.getMessage());
                throw new CommunicationException(exception.getMessage(), exception);
            }
        }

    }

    private static final class OutOfDateCacheHighWaterMark implements CacheHighWaterMark {
        private static final long serialVersionUID = 1L;

        private OutOfDateCacheHighWaterMark() {
        }

        public long getTime() {
            return -1L;
        }

        public boolean isForSameCache(CacheHighWaterMark other) {
            return false;
        }
    }

    public static final class FileStoreException extends EngineException {
        FileStoreException(String message) {
            super(message);
        }

        FileStoreException(String message, Throwable e) {
            super(message, e);
        }
    }
}
