package org.ngrinder.agent.service;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.infra.config.Config;
import org.ngrinder.infra.schedule.ScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.ngrinder.common.util.CollectionUtils.buildMap;
import static org.ngrinder.common.util.CollectionUtils.newHashMap;
import static org.ngrinder.common.util.CompressionUtils.FilePredicate;
import static org.ngrinder.common.util.CompressionUtils.ZipEntryProcessor;
import static org.ngrinder.common.util.CompressionUtils.addFileToTar;
import static org.ngrinder.common.util.CompressionUtils.addFolderToTar;
import static org.ngrinder.common.util.CompressionUtils.addInputStreamToTar;
import static org.ngrinder.common.util.CompressionUtils.processJarEntries;
import static org.ngrinder.common.util.ExceptionUtils.processException;

/**
 * Agent package service.
 *
 * @author Matt
 * @since 3.3
 */
@Service
@PropertySource("classpath:agent-libs.properties")
public class AgentPackageService {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AgentPackageService.class);
    public static final int EXEC = 0x81ed;
    private static final int TIME_MILLIS_OF_DAY = 1000 * 60 * 60 * 24;

    @Autowired
    private Config config;

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Value("#{'${agent.package.libs}'.split(',')}")
    private List<String> agentLibs;

    @PostConstruct
    public void init() {
        // clean up package directories not to occupy too much spaces.
        cleanUpPackageDir(true);
        scheduledTaskService.addFixedDelayedScheduledTask(new Runnable() {
            @Override
            public void run() {
                cleanUpPackageDir(false);
            }
        }, TIME_MILLIS_OF_DAY);
    }

    private void cleanUpPackageDir(boolean all) {
        synchronized (this) {
            final File packagesDir = getPackagesDir();
            final File[] files = packagesDir.listFiles();
            if (files != null) {
                for (File each : files) {
                    if (!each.isDirectory()) {
                        long expiryTimestamp = each.lastModified() + (TIME_MILLIS_OF_DAY * 2);
                        if (all || expiryTimestamp < System.currentTimeMillis()) {
                            FileUtils.deleteQuietly(each);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get package name
     *
     * @param moduleName nGrinder module name.
     * @return String module full name.
     */
    public String getPackageName(String moduleName) {
        return moduleName + "-" + config.getVersion();
    }

    /**
     * Get distributable package name with appropriate extension.
     *
     * @param moduleName   nGrinder sub  module name.
     * @param regionName   region   namee
     * @param connectionIP where it will connect to
     * @param ownerName    owner name
     * @param forWindow    if true, then package type is zip,if false, package type is tar.
     * @return String  module full name.
     */
    public String getDistributionPackageName(String moduleName, String regionName, String connectionIP,
                                             String ownerName,
                                             boolean forWindow) {
        return getPackageName(moduleName) + getFilenameComponent(regionName) + getFilenameComponent(connectionIP) +
            getFilenameComponent(ownerName) + (forWindow ? ".zip" : ".tar");
    }

    private String getFilenameComponent(String value) {
        value = trimToEmpty(value);
        if (isNotEmpty(value)) {
            value = "-" + value;
        }
        return value;
    }

    /**
     * Get the agent package containing folder.
     *
     * @return File  agent package dir.
     */
    public File getPackagesDir() {
        return config.getHome().getSubFile("download");
    }

    /**
     * Create agent package.
     *
     * @return File  agent package.
     */
    public synchronized File createAgentPackage() {
        return createAgentPackage(null, null, config.getControllerPort(), null);
    }

    /**
     * Create agent package.
     *
     * @param connectionIP host ip.
     * @param region       region
     * @param owner        owner
     * @return File  agent package.
     */
    public synchronized File createAgentPackage(String region, String connectionIP, int port, String owner) {
        return createAgentPackage((URLClassLoader) getClass().getClassLoader(), region, connectionIP, port, owner);
    }

    public File createMonitorPackage() {
        synchronized (AgentPackageService.this) {
            File monitorPackagesDir = getPackagesDir();
            if (monitorPackagesDir.mkdirs()) {
                LOGGER.info("{} is created", monitorPackagesDir.getPath());
            }
            final String packageName = getDistributionPackageName("ngrinder-monitor", "", null, "", false);
            File monitorPackage = new File(monitorPackagesDir, packageName);
            if (monitorPackage.exists()) {
                return monitorPackage;
            }
            FileUtils.deleteQuietly(monitorPackage);
            final String basePath = "ngrinder-monitor/";
            final String libPath = basePath + "lib/";
            TarArchiveOutputStream tarOutputStream = null;
            try {
                tarOutputStream = createTarArchiveStream(monitorPackage);
                addFolderToTar(tarOutputStream, basePath);
                addFolderToTar(tarOutputStream, libPath);
                final URLClassLoader classLoader = (URLClassLoader) getClass().getClassLoader();
                Set<String> libs = getMonitorDependentLibs(classLoader);

                for (URL eachUrl : classLoader.getURLs()) {
                    File eachClassPath = new File(eachUrl.getFile());
                    if (!isJar(eachClassPath)) {
                        continue;
                    }
                    if (isAgentDependentLib(eachClassPath, "ngrinder_sh")) {
                        processJarEntries(eachClassPath, new TarArchivingZipEntryProcessor(tarOutputStream, new FilePredicate() {
                            @Override
                            public boolean evaluate(Object object) {
                                ZipEntry zipEntry = (ZipEntry) object;
                                final String name = zipEntry.getName();
                                return name.contains("monitor") && (zipEntry.getName().endsWith("sh") ||
                                    zipEntry.getName().endsWith("bat"));
                            }
                        }, basePath, EXEC));
                    } else if (isMonitorDependentLib(eachClassPath, libs)) {
                        addFileToTar(tarOutputStream, eachClassPath, libPath + eachClassPath.getName());
                    }
                }
                addMonitorConfToTar(tarOutputStream, basePath, config.getMonitorPort());
            } catch (IOException e) {
                LOGGER.error("Error while generating an monitor package" + e.getMessage());
            } finally {
                IOUtils.closeQuietly(tarOutputStream);
            }
            return monitorPackage;
        }
    }

    /**
     * Create agent package
     *
     * @param classLoader  URLClass Loader
     * @param regionName   region
     * @param connectionIP host ip
     * @param port         host port
     * @param owner        owner
     * @return File
     */
    synchronized File createAgentPackage(URLClassLoader classLoader, String regionName, String connectionIP,
                                         int port, String owner) {
        synchronized (AgentPackageService.this) {
            File agentPackagesDir = getPackagesDir();
            if (agentPackagesDir.mkdirs()) {
                LOGGER.info("{} is created", agentPackagesDir.getPath());
            }
            final String packageName = getDistributionPackageName("ngrinder-agent",
                regionName, connectionIP, owner, false);
            File agentTar = new File(agentPackagesDir, packageName);
            if (agentTar.exists()) {
                FileUtils.deleteQuietly(agentTar);
            }
            final String basePath = "ngrinder-agent/";
            final String libPath = basePath + "lib/";
            TarArchiveOutputStream tarOutputStream = null;
            try {
                tarOutputStream = createTarArchiveStream(agentTar);
                addFolderToTar(tarOutputStream, basePath);
                addFolderToTar(tarOutputStream, libPath);
                addAgentConfToTar(tarOutputStream, basePath);
                Set<String> libs = getDependentLibs();
                if ("file".equals(Config.RUN_PROTOCOL)) {
                    fileProtocolPackage(libPath, tarOutputStream, libs, classLoader.getURLs());
                } else if ("jar".equals(Config.RUN_PROTOCOL)) {
                    jarProtocolPackage(libPath, tarOutputStream, libs);
                } else {
                    throw new IOException("Not support url connection.");
                }
            } catch (Exception e) {
                LOGGER.error("Error while generating an agent package" + e.getMessage());
            } finally {
                IOUtils.closeQuietly(tarOutputStream);
            }
            return agentTar;
        }
    }

    private void addAgentConfToTar(TarArchiveOutputStream tarOutputStream, String basePath) {
        try {
            writeFileToTar(tarOutputStream, basePath, "ngrinder_agent_home_template/__agent.conf", TarArchiveEntry.DEFAULT_FILE_MODE);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/run_agent.bat", EXEC);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/run_agent.sh", EXEC);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/run_agent_bg.sh", EXEC);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/run_agent_internal.bat", EXEC);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/run_agent_internal.sh", EXEC);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/stop_agent.bat", EXEC);
            writeFileToTar(tarOutputStream, basePath, "ngrinder_sh/agent/stop_agent.sh", EXEC);
        } catch (Exception e) {
            LOGGER.error("Add config file to agent tar failed when package!");
        }
    }

    private void writeFileToTar(TarArchiveOutputStream tarOutputStream,
                                String basePath,
                                String jarEntryIdentify,
                                int mode) throws IOException {
        // 如果直接拿不到文件，说明是在被依赖的jar包中，所以添加一层打成jar包的依赖目录，这里只作为springboot的依赖路径BOOT-INF/处理，其他情况不考虑
        URL resource = getClass().getClassLoader().getResource(jarEntryIdentify);
        if (resource == null) {
            resource = getClass().getClassLoader().getResource("BOOT-INF/classes/" + jarEntryIdentify);
        }
        if (resource == null) {
            return;
        }
        try (InputStream configInputStream = resource.openStream()) {
            byte[] dataBytes = getDataBytes(configInputStream);
            String fileName = new File(jarEntryIdentify).getName();
            addInputStreamToTar(tarOutputStream, new ByteArrayInputStream(dataBytes), basePath + fileName,
                dataBytes.length, mode);
        }
    }

    private String getMainJarFilePath() {
        URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        String urlPath = url.getFile();
        int indexOfJarFileStart = urlPath.indexOf("!/");
        if (indexOfJarFileStart == -1) {
            return null;
        }
        urlPath = urlPath.substring(0, indexOfJarFileStart);
        urlPath = urlPath.replace("file:", "");
        return urlPath;
    }

    private void jarProtocolPackage(String libPath,
                                    TarArchiveOutputStream tarOutputStream,
                                    Set<String> agentDependencyLibs) throws IOException {
        String urlPath = getMainJarFilePath();
        if (urlPath == null) {
            return;
        }
        if (agentDependencyLibs == null
            || tarOutputStream == null
            || StringUtils.isEmpty(libPath)) {
            return;
        }
        JarFile jarFile = new JarFile(urlPath);
        String jarEntryPrefix = "BOOT-INF/lib/";
        for (String eachDependencyLib : agentDependencyLibs) {
            String eachEntryPath = jarEntryPrefix + eachDependencyLib;
            JarEntry jarEntry = jarFile.getJarEntry(eachEntryPath);
            if (jarEntry == null) {
                LOGGER.error("The {} didn't found when package agent.", eachDependencyLib);
                continue;
            }
            try (InputStream resource = jarFile.getInputStream(jarEntry)) {
                addInputStreamToTar(tarOutputStream, resource, Paths.get(libPath, eachDependencyLib).toString(),
                    jarEntry.getSize(), TarArchiveEntry.DEFAULT_FILE_MODE);
            }
        }
    }

    private byte[] getDataBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new byte[]{};
        }
        StringBuilder sb = new StringBuilder();
        byte[] cache = new byte[4096];
        int length = 0;
        while ((length = inputStream.read(cache)) != -1) {
            sb.append(new String(cache, 0, length, StandardCharsets.UTF_8));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void fileProtocolPackage(String libPath,
                                     TarArchiveOutputStream tarOutputStream,
                                     Set<String> libs,
                                     URL[] urls) throws IOException {
        for (URL url : urls) {
            File eachClassPath = new File(url.getFile());
            if (!isJar(eachClassPath)) {
                return;
            }
            if (!isAgentDependentLib(eachClassPath, libs)) {
                return;
            }
            addFileToTar(tarOutputStream, eachClassPath, libPath + eachClassPath.getName());
        }
    }

    private TarArchiveOutputStream createTarArchiveStream(File agentTar) throws IOException {
        FileOutputStream fos = new FileOutputStream(agentTar);
        return new TarArchiveOutputStream(new BufferedOutputStream(fos));
    }

    private void addMonitorConfToTar(TarArchiveOutputStream tarOutputStream, String basePath,
                                     Integer monitorPort) throws IOException {
        final String config = getAgentConfigContent("agent_monitor.conf", buildMap("monitorPort",
            String.valueOf(monitorPort)));
        final byte[] bytes = config.getBytes();
        addInputStreamToTar(tarOutputStream, new ByteArrayInputStream(bytes), basePath + "__agent.conf",
            bytes.length, TarArchiveEntry.DEFAULT_FILE_MODE);
    }

    private Set<String> getMonitorDependentLibs(URLClassLoader cl) throws IOException {
        Set<String> libs = new HashSet<String>();
        InputStream dependencyStream = null;
        try {
            dependencyStream = cl.getResourceAsStream("monitor-dependencies.txt");
            final String dependencies = IOUtils.toString(dependencyStream);
            for (String each : StringUtils.split(dependencies, ";")) {
                libs.add(FilenameUtils.getBaseName(each.trim()).replace("-SNAPSHOT", ""));
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading monitor-dependencies.txt", e);
        } finally {
            IOUtils.closeQuietly(dependencyStream);
        }
        return libs;
    }

    private Set<String> getDependentLibs() {
        return new HashSet<>(agentLibs);
    }

    /**
     * Check if this given path is jar.
     *
     * @param libFile lib file
     * @return true if it's jar
     */
    public boolean isJar(File libFile) {
        return StringUtils.endsWithAny(libFile.getName(), new String[]{".jar", ".jar!"});
    }

    /**
     * Check if this given lib file is the given library.
     *
     * @param libFile lib file
     * @param libName desirable name
     * @return true if dependent lib
     */
    public boolean isAgentDependentLib(File libFile, String libName) {
        return StringUtils.startsWith(libFile.getName(), libName);
    }

    /**
     * Check if this given lib file in the given lib set.
     *
     * @param libFile lib file
     * @param libs    lib set
     * @return true if dependent lib
     */
    public boolean isMonitorDependentLib(File libFile, Set<String> libs) {
        if (libFile.getName().contains("grinder-3.9.1.jar")) {
            return false;
        }
        String name = libFile.getName();
        name = name.replace(".jar", "").replace("-SNAPSHOT", "");
        for (String each : libs) {
            if (name.contains(each)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this given lib file in the given lib set.
     *
     * @param libFile lib file
     * @param libs    lib set
     * @return true if dependent lib
     */
    public boolean isAgentDependentLib(File libFile, Set<String> libs) {
        if (libFile.getName().contains("grinder-3.9.1.jar")) {
            return false;
        }
        String name = libFile.getName();
        return libs.contains(name);
    }

    /**
     * Get the agent.config content replacing the variables with the given values.
     *
     * @param templateName template name.
     * @param values       map of configurations.
     * @return generated string
     */
    public String getAgentConfigContent(String templateName, Map<String, Object> values) {
        StringWriter writer = null;
        try {
            Configuration config = new Configuration();
            ClassPathResource cpr = new ClassPathResource("ngrinder_agent_home_template");
            config.setDirectoryForTemplateLoading(cpr.getFile());
            config.setObjectWrapper(new DefaultObjectWrapper());
            Template template = config.getTemplate(templateName);
            writer = new StringWriter();
            template.process(values, writer);
            return writer.toString();
        } catch (Exception e) {
            throw processException("Error while fetching the script template.", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    static class TarArchivingZipEntryProcessor implements ZipEntryProcessor {
        private TarArchiveOutputStream tao;
        private FilePredicate filePredicate;
        private String basePath;
        private int mode;

        TarArchivingZipEntryProcessor(TarArchiveOutputStream tao, FilePredicate filePredicate, String basePath, int mode) {
            this.tao = tao;
            this.filePredicate = filePredicate;
            this.basePath = basePath;
            this.mode = mode;
        }

        @Override
        public void process(ZipFile file, ZipEntry entry) throws IOException {
            InputStream inputStream = null;
            try {
                inputStream = file.getInputStream(entry);

                if (filePredicate.evaluate(entry)) {
                    addInputStreamToTar(this.tao, inputStream, basePath + FilenameUtils.getName(entry.getName()),
                        entry.getSize(),
                        this.mode);
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

}
