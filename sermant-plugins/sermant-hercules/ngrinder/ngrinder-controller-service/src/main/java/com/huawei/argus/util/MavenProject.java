/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.argus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 功能描述：获取maven类型脚本中，各部分的模板文件流对象
 *
 * @author zl
 * @since 2022-03-15
 */
public final class MavenProject {
    /**
     * 加载类的classloader
     */
    private static final ClassLoader classLoader = MavenProject.class.getClassLoader();

    /**
     * POM文件路径classpath路径
     */
    private static final String POM_CLASS_PATH = "script_template/groovy_maven/pom.xml";

    /**
     * application.properties文件classpath路径
     */
    private static final String APPLICATION_FILE_PATH = "script_template/groovy_maven/src/main/resources/application.properties";

    /**
     * POM文件路径classpath路径
     */
    private static final String POM_PATH_IN_SCRIPT = "pom.xml";

    /**
     * application.properties文件classpath路径
     */
    private static final String APPLICATION_PATH_IN_SCRIPT = "src/main/resources/application.properties";

    /**
     * 获取pom.xml模板信息
     *
     * @return pom.xml模板信息
     */
    public static MavenFile getPomFile() {
        return new MavenFile(POM_PATH_IN_SCRIPT, getFileContent(POM_CLASS_PATH));
    }

    /**
     * 获取application.properties模板信息
     *
     * @return application.properties模板信息
     */
    public static MavenFile getApplicationPropertiesFile() {
        return new MavenFile(APPLICATION_PATH_IN_SCRIPT, getFileContent(APPLICATION_FILE_PATH));
    }

    /**
     * 获取指定文件模板内容
     *
     * @return 指定模板文件内容
     */
    private static String getFileContent(String fileClassPath) {
        try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileClassPath)) {
            if (resourceAsStream == null) {
                throw new RuntimeException(String.format(Locale.ENGLISH, "The template for %s isn't exist.", fileClassPath));
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8));
            String line;
            StringBuilder templateBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                templateBuilder.append(line);
            }
            return templateBuilder.toString();
        } catch (IOException exception) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "The template for %s isn't exist.", fileClassPath));
        }
    }

    /**
     * 封装模板文件信息
     */
    public static class MavenFile {
        private final String path;
        private final String content;

        public MavenFile(String path, String fileInputStream) {
            this.path = path;
            this.content = fileInputStream;
        }

        public String getPath() {
            return path;
        }

        public String getContent() {
            return content;
        }
    }
}
