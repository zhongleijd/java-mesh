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
package org.ngrinder.common.model;

import com.huawei.argus.util.ClassPathResourceUtil;
import org.apache.commons.io.FileUtils;
import org.ngrinder.common.constants.GrinderConstants;
import org.ngrinder.common.exception.ConfigurationException;
import org.ngrinder.common.util.EncodingUtils;
import org.ngrinder.common.util.NoOp;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import sun.net.www.protocol.file.FileURLConnection;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.util.Properties;
import java.util.jar.JarFile;

import static org.ngrinder.common.util.ExceptionUtils.processException;
import static org.ngrinder.common.util.Preconditions.checkNotNull;

/**
 * Home class which enables the easy resource access in ${NGRINDER_HOME}
 * directory.
 *
 * @author JunHo Yoon
 * @since 3.0
 */
public class Home {

    // HOME_PATH
    private static final String PATH_PLUGIN = "plugins";
    private static final String PATH_SCRIPT = "script";
    private static final String PATH_USER_REPO = "repos";
    private static final String PATH_PERF_TEST = "perftest";
    private static final String PATH_DOWNLOAD = "download";
    private static final String PATH_GLOBAL_LOG = "logs";
    private static final String PATH_LOG = "logs";
    private static final String PATH_REPORT = "report";
    private static final String PATH_DIST = "dist";
    private static final String PATH_STAT = "stat";
    private static final String PATH_AGENT = "agent";
    private final static Logger LOGGER = LoggerFactory.getLogger(Home.class);
    private final File directory;
    public static final String REPORT_CSV = "output.csv";
    public static final String SVN_HOME = "svn://100.95.184.191/all/";

    /**
     * Constructor.
     *
     * @param directory home directory
     */
    public Home(File directory) {
        this(directory, true);
    }

    /**
     * Constructor.
     *
     * @param directory home directory ${NGRINDER_HOME}
     * @param create    create the directory if not exists
     */
    public Home(File directory, boolean create) {
        checkNotNull(directory, "directory should not be null");
        if (create) {
            if (directory.mkdir()) {
                LOGGER.info("{} is created.", directory.getPath());
            }
        }
        if (directory.exists() && !directory.canWrite()) {
            throw new ConfigurationException(String.format(" ngrinder home directory %s is not writable.", directory),
                null);
        }
        this.directory = directory;
    }

    public void init() {
        makeSubPath(PATH_PLUGIN);
        makeSubPath(PATH_PERF_TEST);
        makeSubPath(PATH_DOWNLOAD);
        makeSubPath(PATH_AGENT);
        makeSubPath(PATH_SCRIPT);
    }

    /**
     * Get the home directory.
     *
     * @return the home directory
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Copy the given file from given location.
     *
     * @param dirInJar file location
     */
    public void copyConfigFrom(String dirInJar) {
        ClassPathResource from = new ClassPathResource(dirInJar);
        // Copy missing files
        try {
            URLConnection urlConnection = ClassPathResourceUtil.getUrlConnection(from);
            if (urlConnection instanceof FileURLConnection) {
                copySystemFile(directory, from.getFile());
            } else if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                String distDirPath = directory.getPath();
                ClassPathResourceUtil.copyJarDirToSystemDir(jarFile, dirInJar, "", distDirPath, "conf_template");
            } else {
                LOGGER.error("Invalid class path resource:{}", from.getPath());
            }
        } catch (IOException e) {
            throw processException("Fail to copy files from " + from.getPath(), e);
        }
    }

    /**
     * 复制agent的配置文件到agent的目录.
     *
     * @param agentConfigFile file location
     */
    public void copyAgentConfigFile(String agentConfigFile) {
        ClassPathResource from = new ClassPathResource(agentConfigFile);
        // Copy missing files
        try {
            URLConnection urlConnection = ClassPathResourceUtil.getUrlConnection(from);
            String distDir = directory.getPath();
            String agentDir = ClassPathResourceUtil.combineDir(distDir, PATH_AGENT);
            if (urlConnection instanceof FileURLConnection) {
                copySystemFile(new File(agentDir), from.getFile());
            } else if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                String agentConfFile = agentConfigFile + "__agent.conf";
                ClassPathResourceUtil.copyJarFileToSystemDir(jarFile, agentConfFile, PATH_AGENT, distDir, PATH_AGENT);
            } else {
                LOGGER.error("Invalid class path resource:{}", from.getPath());
            }
        } catch (IOException e) {
            throw processException("Fail to copy files from " + from.getPath(), e);
        }
    }

    /**
     * 复制agent的脚本文件到agent的目录.
     *
     * @param shellFilePath file location
     */
    public void copyAgentShell(String shellFilePath) {
        ClassPathResource from = new ClassPathResource(shellFilePath);
        // Copy missing files
        try {
            URLConnection urlConnection = ClassPathResourceUtil.getUrlConnection(from);
            String distDir = directory.getPath();
            String agentDir = ClassPathResourceUtil.combineDir(distDir, PATH_AGENT);
            if (urlConnection instanceof FileURLConnection) {
                copySystemFile(new File(agentDir), from.getFile());
            } else if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                ClassPathResourceUtil.copyJarDirToSystemDir(jarFile, shellFilePath, PATH_AGENT, distDir, PATH_AGENT);
            } else {
                LOGGER.error("Invalid class path resource:{}", from.getPath());
            }
        } catch (IOException e) {
            throw processException("Fail to copy files from " + from.getPath(), e);
        }
    }

    /**
     * 复制本地文件
     *
     * @param distDir 目标文件夹
     * @param from    需要复制的文件
     * @throws IOException 文件异常
     */
    private void copySystemFile(File distDir, File from) throws IOException {
        for (File file : checkNotNull(from.listFiles())) {
            if (!(new File(distDir, file.getName()).exists())) {
                FileUtils.copyFileToDirectory(file, distDir);
                continue;
            }
            File orgConf = new File(distDir, "conf_template");
            FileUtils.copyFile(file, new File(orgConf, file.getName()));
        }
    }

    /**
     * Make a sub directory on the home directory.
     *
     * @param subPathName sub-path name
     */
    public void makeSubPath(String subPathName) {
        mkDir(new File(directory, subPathName));
    }

    /**
     * Get the {@link Properties} from the the given configuration file.
     *
     * @param confFileName configuration file name
     * @return loaded {@link Properties}
     */
    public Properties getProperties(String confFileName) {
        try {
            File configFile = getSubFile(confFileName);
            if (configFile.exists()) {
                byte[] propByte = FileUtils.readFileToByteArray(configFile);
                String propString = EncodingUtils.getAutoDecodedString(propByte, "UTF-8");
                Properties prop = new Properties();
                prop.load(new StringReader(propString));
                return prop;
            } else {
                // default empty properties.
                return new Properties();
            }

        } catch (IOException e) {
            throw processException("Fail to load property file " + confFileName, e);
        }
    }

    /**
     * Get the sub {@link File} instance under the home directory.
     *
     * @param subPathName subpath name
     * @return {@link File}
     */
    public File getSubFile(String subPathName) {
        return new File(directory, subPathName);
    }

    /**
     * Get the plugin cache directory.
     *
     * @return plugin cache directory.
     */
    public File getPluginsCacheDirectory() {
        File cacheDir = getSubFile(PATH_PLUGIN + "_cache");
        cacheDir.mkdirs();
        return cacheDir;
    }

    /**
     * Get the plugin directory.
     *
     * @return plugin directory.
     */
    public File getPluginsDirectory() {
        return getSubFile(PATH_PLUGIN);
    }

    /**
     * Get the repo base directory.
     *
     * @return repo base directory.
     */
    public File getRepoDirectoryRoot() {
        return getSubFile(PATH_USER_REPO);
    }

    /**
     * Get the user repo directory for the given user.
     *
     * @param user user
     * @return user repo directory.
     */
    public File getUserRepoDirectory(User user) {
        return getUserRepoDirectory(user.getUserId());
    }

    /**
     * Get the sub directory of the base user repo directory.
     *
     * @param subPath subPath
     * @return base repo sub directory.
     */
    public File getUserRepoDirectory(String subPath) {
        return new File(getRepoDirectoryRoot(), subPath);
    }

    /**
     * 获取SVN仓库根路径
     *
     * @return
     */
    public String getSvnRepoPath() {
        return SVN_HOME;
    }

    /**
     * 获取用户的SVN文件夹路径
     *
     * @param user
     * @return
     */
    public String getUserSvnRepoPath(User user) {
        return SVN_HOME + user.getUserName();
    }

    /**
     * Get the base perftest directory.
     *
     * @return base perftest directory.
     */
    public File getPerfTestDirectory() {
        return getSubFile(PATH_PERF_TEST);
    }

    /**
     * Get the sub directory for the given perftest.
     *
     * @param perfTest perfTest
     * @param subPath  subPath
     * @return {@link PerfTest} sub directory.
     */
    private File getPerfTestSubDirectory(PerfTest perfTest, String subPath) {
        return mkDir(new File(getPerfTestDirectory(perfTest), subPath));
    }

    private File mkDir(File file) {
        if (file.mkdirs()) {
            LOGGER.info("{} is created.", file.getPath());
        }
        return file;
    }

    /**
     * Get the sub directory of the given perftest's base directory.
     *
     * @param id      perfTest id
     * @param subPath subPath
     * @return {@link PerfTest} sub directory.
     */
    public File getPerfTestSubDirectory(String id, String subPath) {
        File file = new File(getPerfTestDirectory(id), subPath);
        return mkDir(file);
    }

    /**
     * Get the perftest base directory for the given perftest id.
     *
     * @param id perftest id
     * @return {@link PerfTest} sub directory.
     */
    public File getPerfTestDirectory(String id) {
        File file = new File(getPerfTestDirectory(), id);
        // For backward compatibility
        if (!file.exists()) {
            file = getDistributedFolderName(id);
        }
        return mkDir(file);
    }

    File getDistributedFolderName(String id) {
        int numericId = 0;
        try {
            numericId = (Integer.parseInt(id) / 1000) * 1000;
        } catch (NumberFormatException e) {
            NoOp.noOp();
        }
        String folderName = String.format("%d_%d%s%s", numericId, numericId + 999, File.separator, id);
        return new File(getPerfTestDirectory(), folderName);
    }

    /**
     * Get the root directory for given {@link PerfTest} id.
     *
     * @param perfTest perftest
     * @return {@link PerfTest} log directory
     */
    public File getPerfTestDirectory(PerfTest perfTest) {
        return getPerfTestDirectory(String.valueOf(perfTest.getId()));
    }

    /**
     * Get the log directory for given {@link PerfTest} id.
     *
     * @param id perftest id
     * @return {@link PerfTest} log directory
     */
    public File getPerfTestLogDirectory(String id) {
        return getPerfTestSubDirectory(id, PATH_LOG);
    }

    /**
     * Get the log directory for given {@link PerfTest}.
     *
     * @param perfTest perftest
     * @return {@link PerfTest} log directory
     */
    public File getPerfTestLogDirectory(PerfTest perfTest) {
        return getPerfTestSubDirectory(perfTest, PATH_LOG);
    }

    /**
     * Get the distribution directory for given {@link PerfTest}.
     *
     * @param perfTest perftest
     * @return {@link PerfTest} distribution directory
     */
    public File getPerfTestDistDirectory(PerfTest perfTest) {
        return getPerfTestSubDirectory(perfTest, PATH_DIST);
    }

    /**
     * Get the statistics directory for given {@link PerfTest}.
     *
     * @param perfTest perftest
     * @return {@link PerfTest} statistics directory
     */
    public File getPerfTestStatisticPath(PerfTest perfTest) {
        return getPerfTestSubDirectory(perfTest, PATH_STAT);
    }

    /**
     * Get the report directory for given {@link PerfTest} id.
     *
     * @param id perftest id
     * @return {@link PerfTest} report directory
     */
    public File getPerfTestReportDirectory(String id) {
        return getPerfTestSubDirectory(id, PATH_REPORT);
    }

    /**
     * Get the report directory for given {@link PerfTest}.
     *
     * @param perfTest perftest
     * @return {@link PerfTest} report directory
     */
    public File getPerfTestReportDirectory(PerfTest perfTest) {
        return getPerfTestSubDirectory(perfTest, PATH_REPORT);
    }

    /**
     * Get the csv file for given {@link PerfTest}.
     *
     * @param perfTest perftest
     * @return {@link PerfTest} csv file
     */
    public File getPerfTestCsvFile(PerfTest perfTest) {
        return new File(getPerfTestReportDirectory(perfTest), REPORT_CSV);
    }

    /**
     * Get the default grinder properties file.
     *
     * @return grinder properties file
     */
    public File getDefaultGrinderProperties() {
        return getSubFile(GrinderConstants.DEFAULT_GRINDER_PROPERTIES);
    }

    /**
     * Get the download directory.
     *
     * @return download directory
     */
    public File getDownloadDirectory() {
        return getSubFile(PATH_DOWNLOAD);
    }


    /**
     * Get global log file.
     *
     * @return log file
     */
    public File getGlobalLogFile() {
        return getSubFile(PATH_GLOBAL_LOG);
    }

    /**
     * Check if this home exists.
     *
     * @return true if exists.
     */
    public boolean exists() {
        return directory.exists();
    }

    /**
     * Get the user defined messages directory.
     *
     * @return the user defined messages directory
     */
    public File getMessagesDirectory() {
        return getSubFile("messages");
    }

    /**
     * Get the script directory for the given user.
     *
     * @param user user
     * @return script directory for the given user.
     */
    public File getScriptDirectory(User user) {
        return new File(getSubFile(PATH_SCRIPT), user.getUserId());
    }

    /**
     * Get the script directory for the given user.
     *
     * @return script directory for the given user.
     */
    public File getScriptDirectory() {
        return getSubFile(PATH_SCRIPT);
    }
}