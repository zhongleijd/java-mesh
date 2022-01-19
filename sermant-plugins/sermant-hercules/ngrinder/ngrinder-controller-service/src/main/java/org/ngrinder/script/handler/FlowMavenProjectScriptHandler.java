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
import org.ngrinder.common.util.PathUtils;
import org.ngrinder.common.util.UrlUtils;
import org.ngrinder.model.User;
import org.ngrinder.script.model.FileCategory;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.service.NfsFileEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.ngrinder.common.util.CollectionUtils.newHashMap;
import static org.ngrinder.common.util.ExceptionUtils.processException;

/**
 * @author x00377290
 * @since 20190428
 */
@Component
public class FlowMavenProjectScriptHandler extends GroovyMavenProjectScriptHandler implements ProjectHandler {
    @Autowired
    private NfsFileEntryService nfsFileEntryService;

    /**
     * Constructor.
     */
    public FlowMavenProjectScriptHandler() {
        super("argus_flow", "argus_flow", "Argus Flow", "argus_flow");
    }

    @Override
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
                } else if (subpath.endsWith("sceneJsonData.txt")) {
                    fileContent = "";
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
                nfsFileEntryService.saveFile(user, fileEntry);
            } catch (IOException e) {
                throw processException("Error while saving " + each.getName(), e);
            }
        }
    }
}
