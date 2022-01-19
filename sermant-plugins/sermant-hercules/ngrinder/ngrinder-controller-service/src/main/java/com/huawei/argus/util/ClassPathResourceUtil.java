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

package com.huawei.argus.util;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 功能描述：处理jar文件资源
 *
 * @author zl
 * @since 2022-01-15
 */
public final class ClassPathResourceUtil {
    /**
     * 复制资源中的文件到指定路径
     *
     * @param jarFile        文件资源实例
     * @param fileInResource 需要复制的文件资源实例中的文件路径
     * @param pathNameInDist 在目标路径中的文件夹路径
     * @param distDir        需要复制到系统的文件夹
     * @param templateDir    如果目标文件已经存在，则放此目录里面
     */
    public static void copyJarFileToSystemDir(JarFile jarFile,
                                              String fileInResource,
                                              String pathNameInDist,
                                              String distDir,
                                              String templateDir) throws IOException {
        JarEntry fileInResourceJarEntry = jarFile.getJarEntry(fileInResource);
        if (fileInResourceJarEntry == null) {
            throw new IOException("Can not find file in jar.");
        }
        if (fileInResourceJarEntry.isDirectory()) {
            makeDirs(combineDir(distDir, pathNameInDist));
            return;
        }
        File tmpFile = new File(fileInResource);
        String fileName = tmpFile.getName();
        String allPath = combineDir(distDir, pathNameInDist);
        boolean makeDirs = makeDirs(allPath);
        if (!makeDirs) {
            throw new IOException("Create dir fail:" + allPath);
        }
        File distFile = new File(allPath, fileName);
        if (distFile.exists()) {
            allPath = combineDir(distDir, templateDir);
            distFile = new File(allPath, fileName);
        }
        try (InputStream inputStream = jarFile.getInputStream(fileInResourceJarEntry)) {
            FileUtils.copyInputStreamToFile(inputStream, distFile);
        }
    }

    /**
     * 复制jar文件中一个路径下面的所有文件到指定的本地文件夹
     *
     * @param jarFile        文件资源
     * @param dirInResource  需要被复制的jar中目录地址
     * @param pathNameInDist 在目标路径中的文件夹路径
     * @param distDir        需要复制的目的地文件夹
     * @param templateDir    如果需要复制的文件在目标文件夹已经存在，这复制到模板文件夹
     */
    public static void copyJarDirToSystemDir(JarFile jarFile,
                                             String dirInResource,
                                             String pathNameInDist,
                                             String distDir,
                                             String templateDir) throws IOException {
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String entryPath = jarEntry.getName();
            if (!entryPath.startsWith(dirInResource)) {
                continue;
            }
            copyJarFileToSystemDir(jarFile, entryPath, pathNameInDist, distDir, templateDir);
        }
    }

    /**
     * 创建逐级文件目录
     *
     * @param dirPath 文件逐级目录
     * @return 创建是否成功
     */
    public static boolean makeDirs(String dirPath) {
        File dirPathFile = new File(dirPath);
        if (dirPathFile.exists()) {
            return true;
        }
        return dirPathFile.mkdirs();
    }

    /**
     * 合并两个文件夹路径
     *
     * @param parent 父目录
     * @param child  子目录
     * @return 合并之后的目录
     */
    public static String combineDir(String parent, String child) {
        if (StringUtils.isEmpty(child)) {
            return parent;
        }
        if (StringUtils.isEmpty(parent)) {
            return child.startsWith(File.separator) ? child : File.separator + child;
        }
        if (parent.endsWith(File.separator) || parent.endsWith("/")) {
            parent = parent.substring(0, parent.length() - 1);
        }
        if (child.startsWith(File.separator) || child.startsWith("/")) {
            child = child.substring(1);
        }
        return parent + File.separator + child;
    }

    /**
     * 从{@link ClassPathResource}中获取{@link URLConnection}
     *
     * @param classPathResource 文件资源
     * @return 连接
     * @throws IOException 操作异常
     */
    public static URLConnection getUrlConnection(ClassPathResource classPathResource) throws IOException {
        if (classPathResource == null) {
            throw new IOException("The class path resource is null.");
        }
        URL pathResourceURL = classPathResource.getURL();
        if (pathResourceURL == null) {
            throw new IOException("The class path resource is not exist.");
        }
        return pathResourceURL.openConnection();
    }
}
