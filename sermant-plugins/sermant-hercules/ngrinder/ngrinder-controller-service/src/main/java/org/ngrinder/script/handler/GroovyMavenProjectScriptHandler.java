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
package org.ngrinder.script.handler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.ngrinder.common.util.PathUtils;
import org.ngrinder.common.util.PropertiesWrapper;
import org.ngrinder.common.util.UrlUtils;
import org.ngrinder.model.User;
import org.ngrinder.script.model.FileCategory;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.model.FileType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.ngrinder.common.util.CollectionUtils.buildMap;
import static org.ngrinder.common.util.CollectionUtils.newArrayList;
import static org.ngrinder.common.util.CollectionUtils.newHashMap;
import static org.ngrinder.common.util.ExceptionUtils.processException;

/**
 * Groovy Maven project {@link ScriptHandler}.
 *
 * @author JunHo Yoon
 * @since 3.2
 */
@Component
public class GroovyMavenProjectScriptHandler extends GroovyScriptHandler implements ProjectHandler {

    /**
     * Constructor.
     */
    public GroovyMavenProjectScriptHandler() {
        super("groovy_maven", "", "Groovy Maven Project", "groovy");
    }

    public GroovyMavenProjectScriptHandler(String key, String extension, String title, String codeMirrorKey) {
        super(key, extension, title, codeMirrorKey);
    }

    private static final String RESOURCES = buildPathString("src", "main", "resources");
    private static final String JAVA = buildPathString("src", "main", "java");
    private static final String GROOVY = buildPathString("src", "main", "groovy");
    private static final String LIB = buildPathString("lib");

    private static String buildPathString(String start, String... pathPartitions) {
        String path = Paths.get(start, pathPartitions).toString();
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    public boolean canHandle(FileEntry fileEntry) {
        if (fileEntry.getCreatedUser() == null) {
            return false;
        }
        String path = fileEntry.getPath();
        if (!FilenameUtils.isExtension(path, "groovy")) {
            return false;

        }
        //noinspection SimplifiableIfStatement
        if (!path.contains(JAVA) && !path.contains(GROOVY)) {
            return false;
        }

        try {
            FileEntry pomFileEntry = getNfsFileEntryService().getSpecifyScript(fileEntry.getCreatedUser(), getBasePath(path) + "/pom.xml");
            return pomFileEntry != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Integer displayOrder() {
        return 400;
    }

    @Override
    protected Integer order() {
        return 200;
    }

    @Override
    public List<FileEntry> getLibAndResourceEntries(User user, FileEntry scriptEntry, long revision) {
        List<FileEntry> fileList = newArrayList();
        String basePath = getBasePath(scriptEntry);
        try {
            for (FileEntry eachFileEntry : getNfsFileEntryService().getUserScriptAllFiles(user, basePath + RESOURCES)) {
                FileType fileType = eachFileEntry.getFileType();
                if (fileType.isResourceDistributable()) {
                    fileList.add(eachFileEntry);
                }
            }

            for (FileEntry eachFileEntry : getNfsFileEntryService().getUserScriptAllFiles(user, basePath + JAVA)) {
                FileType fileType = eachFileEntry.getFileType();

                if (fileType.isLibDistributable() && !eachFileEntry.getPath().equals(scriptEntry.getPath())) {
                    fileList.add(eachFileEntry);
                }
            }

            for (FileEntry eachFileEntry : getNfsFileEntryService().getUserScriptAllFiles(user, basePath + GROOVY)) {
                FileType fileType = eachFileEntry.getFileType();
                if (fileType.isLibDistributable() && !eachFileEntry.getPath().equals(scriptEntry.getPath())) {
                    fileList.add(eachFileEntry);
                }
            }

            for (FileEntry eachFileEntry : getNfsFileEntryService().getUserScriptAllFiles(user, basePath + LIB)) {
                FileType fileType = eachFileEntry.getFileType();
                if (fileType.isLibDistributable()) {
                    fileList.add(eachFileEntry);
                }
            }
            fileList.add(getNfsFileEntryService().getSpecifyScript(user, basePath + File.separator + "pom.xml"));
        } catch (IOException e) {
            LOGGER.error("Query maven script fail.");
        }

        return fileList;
    }

    @Override
    protected String calcDistSubPath(String basePath, FileEntry each) {
        String calcDistSubPath = super.calcDistSubPath(basePath, each);
        if (calcDistSubPath.startsWith(JAVA)) {
            return calcDistSubPath.substring(JAVA.length());
        } else if (calcDistSubPath.startsWith(GROOVY)) {
            return calcDistSubPath.substring(GROOVY.length());
        } else if (calcDistSubPath.startsWith(RESOURCES)) {
            return calcDistSubPath.substring(RESOURCES.length());
        }
        return calcDistSubPath;
    }

    @Override
    protected void prepareDistMore(Long testId, User user, FileEntry script, File distDir,
                                   PropertiesWrapper properties, ProcessingResultPrintStream processingResult) {
        String pomPathInSVN = PathUtils.join(getBasePath(script), "pom.xml");
        MavenCli cli = new MavenCli();
        processingResult.println("\nCopy dependencies by running 'mvn dependency:copy-dependencies"
            + " -DoutputDirectory=./lib -DexcludeScope=provided'");

        int result = cli.doMain(new String[]{ // goal specification
            "dependency:copy-dependencies", // run dependency goal
            "-DoutputDirectory=./lib", // to the lib folder
            "-DexcludeScope=provided" // but exclude the provided
            // library
        }, distDir.getAbsolutePath(), processingResult, processingResult);
        boolean success = (result == 0);
        if (success) {
            processingResult.printf("\nDependencies in %s was copied.\n", pomPathInSVN);
            LOGGER.info("Dependencies in {} is copied into {}/lib folder", pomPathInSVN, distDir.getAbsolutePath());
        } else {
            processingResult.printf("\nDependencies copy in %s is failed.\n", pomPathInSVN);
            LOGGER.error("Dependencies copy in {} is failed.", pomPathInSVN);
        }
        // Then it's not necessary to include pom.xml anymore.
        FileUtils.deleteQuietly(new File(distDir, "pom.xml"));
        processingResult.setSuccess(success);
    }

    @Override
    public boolean prepareScriptEnv(User user, String path, String fileName, String name, // LF
                                    String url, boolean createLib, String scriptContent) {
        path = PathUtils.join(path, fileName);
        try {
            // Create Dir entry
            createBaseDirectory(user, path);
            // Create each template entries
            createFileEntries(user, path, name, url, scriptContent);
            if (createLib) {
                createLibraryDirectory(user, path);
            }
        } catch (IOException e) {
            throw processException("Error while patching script_template", e);
        }
        return false;
    }

    private void createLibraryDirectory(User user, String path) throws IOException {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setPath(path + "/lib");
        fileEntry.setFileType(FileType.DIR);
        fileEntry.setDescription("put private libraries here");
        getNfsFileEntryService().saveFile(user, fileEntry);
    }

    protected void createFileEntries(User user, String path, String name, String url,
                                     String scriptContent) throws IOException {
        File scriptTemplateDir;
        scriptTemplateDir = new ClassPathResource("/script_template/" + getKey()).getFile();
        for (File each : FileUtils.listFiles(scriptTemplateDir, null, true)) {
            try {
                String subpath = each.getPath().substring(scriptTemplateDir.getPath().length());
                String fileContent = FileUtils.readFileToString(each, "UTF8");
                if (subpath.endsWith("TestRunner.groovy")) {
                    fileContent = scriptContent;
                } else {
                    fileContent = fileContent.replace("${userName}", user.getUserName());
                    fileContent = fileContent.replace("${name}", name);
                    fileContent = fileContent.replace("${url}", url);
                }
                FileEntry fileEntry = new FileEntry();
                fileEntry.setContent(fileContent);
                fileEntry.setPath(FilenameUtils.normalize(PathUtils.join(path, subpath), true));
                fileEntry.setDescription("create groovy maven project");
                String hostName = UrlUtils.getHost(url);
                if (StringUtils.isNotEmpty(hostName)
                    && fileEntry.getFileType().getFileCategory() == FileCategory.SCRIPT) {
                    Map<String, String> properties = newHashMap();
                    properties.put("targetHosts", UrlUtils.getHost(url));
                    fileEntry.setProperties(properties);
                }
                getNfsFileEntryService().saveFile(user, fileEntry);
            } catch (IOException e) {
                throw processException("Error while saving " + each.getName(), e);
            }
        }
    }

    private void createBaseDirectory(User user, String path) throws IOException {
        FileEntry dirEntry = new FileEntry();
        dirEntry.setPath(path);
        // Make it eclipse default folder ignored.
        dirEntry.setProperties(buildMap("svn:ignore", ".project\n.classpath\n.settings\ntarget"));
        dirEntry.setFileType(FileType.DIR);
        dirEntry.setDescription("create groovy maven project");
        getNfsFileEntryService().saveFile(user, dirEntry);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ngrinder.script.handler.ScriptHandler#getBasePath(java.lang.String)
     */
    @Override
    public String getBasePath(String path) {
        String parentPath;
        if (path.contains(JAVA)) {
            parentPath = path.substring(0, path.lastIndexOf(JAVA));
        } else {
            parentPath = path.substring(0, path.lastIndexOf(GROOVY));
        }
        if (!parentPath.startsWith(File.separator)) {
            parentPath = File.separator + parentPath;
        }
        if (parentPath.endsWith(File.separator)) {
            parentPath = parentPath.substring(0, parentPath.length() - 1);
        }
        return parentPath;
    }

    @Override
    public String getScriptExecutePath(String path) {
        if (path.contains(JAVA)) {
            return path.substring(path.lastIndexOf(JAVA) + JAVA.length());
        } else if (path.contains(GROOVY)) {
            return path.substring(path.lastIndexOf(GROOVY) + GROOVY.length());
        } else {
            return "";
        }
    }

    @Override
    public FileEntry getDefaultQuickTestFilePath(String path) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setPath(path + JAVA + "TestRunner.groovy");
        return fileEntry;
    }

}