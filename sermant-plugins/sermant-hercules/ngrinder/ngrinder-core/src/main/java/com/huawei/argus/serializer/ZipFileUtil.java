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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 功能描述：
 *
 * @author zl
 * @since 2022-01-25
 */
public class ZipFileUtil {

    /**
     * 添加指定文件到压缩文件里面
     *
     * @param zipFileName 压缩包名字
     * @param needZipFile 需要压缩的文件所在的路径
     * @throws IOException io异常
     */
    public static void zipFile(String zipFileName, File needZipFile) throws IOException {
        if (needZipFile == null || !needZipFile.exists()) {
            return;
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(zipFileName);
             ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            doZip(zipOutputStream, needZipFile, "");
        }
    }

    /**
     * 实际执行把需要压缩文件的内容输出到zip压缩输出流中
     *
     * @param zipOutputStream 压缩包输出流
     * @param file            需要被压缩的文件
     * @param zipFilePath     压缩包中的路径
     * @throws IOException io异常
     */
    private static void doZip(ZipOutputStream zipOutputStream, File file, String zipFilePath) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        String fileName = file.getName();
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                return;
            }
            Path basePath = Paths.get(zipFilePath, fileName, "/");
            for (File childFile : childFiles) {
                doZip(zipOutputStream, childFile, basePath.toString());
            }
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            int length;
            byte[] cache = new byte[4096];
            Path basePath = Paths.get(zipFilePath, fileName);
            zipOutputStream.putNextEntry(new ZipEntry(basePath.toString()));
            while ((length = fileInputStream.read(cache)) != -1) {
                zipOutputStream.write(cache, 0, length);
            }
        }
    }

    /**
     * 解压zip文件到指定文件夹
     *
     * @param needUnzipFile 需要被解压的文件
     * @param unzipPath     解压文件存放的路径
     * @throws IOException 文件异常
     */
    public static void unzipFile(File needUnzipFile, String unzipPath) throws IOException {
        if (needUnzipFile == null || !needUnzipFile.exists()) {
            return;
        }
        if (unzipPath == null) {
            unzipPath = "";
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(needUnzipFile))) {
            ZipEntry nextEntry;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = nextEntry.getName();
                Path pathInSystem = Paths.get(unzipPath, entryName);
                doUnzip(zipInputStream, nextEntry, pathInSystem);
            }
        }
    }

    /**
     * 解压zip文件到指定文件夹
     *
     * @param needUnzipFile 需要被解压的文件
     * @param unzipPath     解压文件存放的路径
     * @throws IOException 文件异常
     */
    public static void unzipFile(File needUnzipFile, File unzipPath) throws IOException {
        if (needUnzipFile == null || !needUnzipFile.exists()) {
            return;
        }
        if (unzipPath == null) {
            return;
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(needUnzipFile))) {
            ZipEntry nextEntry;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = nextEntry.getName();
                Path pathInSystem = Paths.get(unzipPath.getPath(), entryName);
                doUnzip(zipInputStream, nextEntry, pathInSystem);
            }
        }
    }

    private static void doUnzip(ZipInputStream zipInputStream, ZipEntry nextEntry, Path pathInSystem) throws IOException {
        if (nextEntry.isDirectory()) {
            Files.createDirectories(pathInSystem);
            return;
        }
        File fileInSystem = pathInSystem.toFile();
        Files.createDirectories(pathInSystem.getParent());
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileInSystem)) {
            int length;
            byte[] cache = new byte[4096];
            while ((length = zipInputStream.read(cache)) != -1) {
                fileOutputStream.write(cache, 0, length);
            }
        }
    }

    /**
     * 删除一个文件夹下面的所有子文件和文件夹
     *
     * @param file 需要删除子文件和文件夹的文件夹
     * @throws IOException IO异常
     */
    public static void deleteChildFile(File file) throws IOException {
        if (file == null || file.isFile()) {
            return;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File childFile : files) {
            Path childFilePath = Paths.get(childFile.getPath());
            if(childFile.isFile()) {
                Files.deleteIfExists(childFilePath);
                continue;
            }
            deleteChildFile(childFile);
            Files.deleteIfExists(childFilePath);
        }
    }
}
