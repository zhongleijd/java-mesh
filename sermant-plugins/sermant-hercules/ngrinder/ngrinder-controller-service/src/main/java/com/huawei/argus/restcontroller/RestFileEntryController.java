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

package com.huawei.argus.restcontroller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.controller.RestAPI;
import org.ngrinder.common.util.HttpContainerContext;
import org.ngrinder.common.util.PathUtils;
import org.ngrinder.common.util.UrlUtils;
import org.ngrinder.model.User;
import org.ngrinder.script.handler.ProjectHandler;
import org.ngrinder.script.handler.ScriptHandler;
import org.ngrinder.script.model.FileCategory;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.model.FileType;
import org.ngrinder.script.service.NfsFileEntryService;
import org.ngrinder.script.service.ScriptValidationService;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.io.FilenameUtils.getPath;
import static org.ngrinder.common.util.EncodingUtils.encodePathWithUTF8;
import static org.ngrinder.common.util.ExceptionUtils.processException;
import static org.ngrinder.common.util.Preconditions.checkNotNull;

@RestController
@RequestMapping("/rest/script")
public class RestFileEntryController extends RestBaseController {

    private static final Logger LOG = LoggerFactory.getLogger(RestFileEntryController.class);

    @Autowired
    private NfsFileEntryService fileEntryService;

    @Autowired
    private ScriptValidationService scriptValidationService;

    @Autowired
    HttpContainerContext httpContainerContext;

    /**
     * Get the list of file entries for the given user.
     *
     * @param user current user
     * @param path path looking for.
     * @return script/list
     */
    @RequestMapping({"/list"})
    public JSONObject getAllListModel(User user, @RequestParam(required = false) String path) {
        path = StringUtils.trimToEmpty(path);
        JSONObject modelInfos = new JSONObject();
        List<FileEntry> allFiles = getAllFiles(user, path);
        for (FileEntry oneFile : allFiles) {
            oneFile.setContent("");
            oneFile.setContentBytes(new byte[]{});
        }
        modelInfos.put("files", allFiles);
        return modelInfos;
    }

    /**
     * Get the script path BreadCrumbs HTML string.
     *
     * @param path path
     * @return generated HTML
     */
    public String getScriptPathBreadcrumbs(String path) {
        String contextPath = httpContainerContext.getCurrentContextUrlFromUserRequest();
        String[] parts = StringUtils.split(path, '/');
        StringBuilder accumulatedPart = new StringBuilder(contextPath).append("/script/list");
        StringBuilder returnHtml = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String each = parts[i];
            accumulatedPart.append("/").append(each);
            if (i != parts.length - 1) {
                returnHtml.append("<a target='_path_view' href='").append(accumulatedPart).append("'>").append(each)
                    .append("</a>").append("/");
            } else {
                returnHtml.append(each);
            }
        }
        return returnHtml.toString();
    }

    /**
     * Add a folder on the given path.
     *
     * @param user       current user
     * @param path       path in which folder will be added
     * @param folderName folderName
     * @return redirect:/script/${user}/${path}
     */
    @RequestMapping(value = "/new/folder", method = RequestMethod.POST)
    public Map<String, Object> addFolder(User user, @RequestParam String path, @RequestParam("folderName") String folderName) { // "fileName"
        fileEntryService.addFolder(user, StringUtils.trimToEmpty(path), StringUtils.trimToEmpty(folderName));
        return success();
    }

    /**
     * 创建一个新的脚本
     *
     * @param user     创建用户
     * @param filePath 脚本所在的路径
     * @param content  脚本内容
     * @return 响应
     */
    @RequestMapping(value = "/new/script", method = RequestMethod.POST)
    public Map<String, Object> createNewScript(User user, @RequestParam String filePath, @RequestBody String content) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setCreatedUser(user);
        fileEntry.setContent(content);
        fileEntry.setContentBytes(content.getBytes(StandardCharsets.UTF_8));
        fileEntry.setPath(filePath);
        try {
            fileEntryService.saveFile(user, fileEntry);
        } catch (IOException e) {
            return fail();
        }
        return success();
    }

    /**
     * Get the details of given path.
     *
     * @param user user
     * @param path user
     * @return script/editor
     */
    @RequestMapping("/detail")
    public FileEntry getOne(User user, @RequestParam String path) throws IOException {
        FileEntry script = fileEntryService.getSpecifyScript(user, path);
        script.setContent("");
        script.setContentBytes(new byte[]{});
        return script;
    }

    /**
     * Download file entry of given path.
     *
     * @param user     current user
     * @param path     user
     * @param response response
     */
    @RequestMapping("/download")
    public void download(User user, @RequestParam String path, HttpServletResponse response) throws IOException {
        FileEntry fileEntry = fileEntryService.getSpecifyScript(user, path);
        if (fileEntry == null) {
            LOG.error("{} requested to download not existing file entity {}", user.getUserId(), path);
            return;
        }
        response.reset();
        try {
            response.addHeader(
                "Content-Disposition",
                "attachment;filename="
                    + java.net.URLEncoder.encode(FilenameUtils.getName(fileEntry.getPath()), "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            LOG.error(e1.getMessage(), e1);
        }
        response.setContentType("application/octet-stream; charset=UTF-8");
        response.addHeader("Content-Length", "" + fileEntry.getFileSize());
        OutputStream toClient = null;
        try {
            toClient = new BufferedOutputStream(response.getOutputStream());
            toClient.write(fileEntry.getContentBytes());
            toClient.flush();
        } catch (IOException e) {
            throw processException("error while download file", e);
        } finally {
            IOUtils.closeQuietly(toClient);
        }
    }

    /**
     * Search files on the query.
     *
     * @param user  current user
     * @param query query string
     * @return script/list
     */
    @RequestMapping(value = "/search")
    public JSONObject search(User user, @RequestParam String query) throws IOException {
        final String trimmedQuery = StringUtils.trimToEmpty(query);
        List<FileEntry> searchResult = fileEntryService.getUserScriptAllFiles(user, "/")
            .stream()
            .filter(input -> input != null
                && input.getFileType() != FileType.DIR
                && StringUtils.containsIgnoreCase(new File(input.getPath()).getName(), trimmedQuery))
            .collect(Collectors.toList());
        JSONObject modelInfos = new JSONObject();
        modelInfos.put("query", query);
        modelInfos.put("files", searchResult);
        modelInfos.put("currentPath", "");
        return modelInfos;
    }

    /**
     * 判断脚本是否存在
     *
     * @param user 当前用户
     * @param path 脚本全路径
     * @return 判断结果
     */
    @RequestMapping(value = "/hasScript", method = RequestMethod.GET)
    boolean hasScript(User user, @RequestParam String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        FileEntry file = null;
        try {
            file = fileEntryService.getSpecifyScript(user, path);
        } catch (IOException e) {
            return false;
        }
        return file != null;
    }

    /**
     * Upload a file.
     *
     * @param user current user
     * @param path path
     * @param file multi part file
     * @return redirect:/script/list/${path}
     */
    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadFile(User user, @RequestParam String path, @RequestPart("uploadFile") MultipartFile file) {
        try {
            upload(user, path, file);
            return encodePathWithUTF8(path);
        } catch (IOException e) {
            LOG.error("Error while getting file content: {}", e.getMessage(), e);
            throw processException("Error while getting file content:" + e.getMessage(), e);
        }
    }

    private void upload(User user, String path, MultipartFile file) throws IOException {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setContentBytes(file.getBytes());
        fileEntry.setPath(FilenameUtils.separatorsToUnix(FilenameUtils.concat(path, file.getOriginalFilename())));
        fileEntryService.saveFile(user, fileEntry);
    }

    /**
     * Delete files on the given path.
     *
     * @param user        user
     * @param path        base path
     * @param filesString file list delimited by ","
     * @return json string
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String delete(User user, @RequestParam String path, @RequestParam("filesString") String filesString) {
        String[] files = filesString.split(",");
        fileEntryService.deleteFile(user, path, files);
        Map<String, Object> rtnMap = new HashMap<String, Object>(1);
        rtnMap.put(JSON_SUCCESS, true);
        return toJson(rtnMap);
    }

    /**
     * Create the given file.
     *
     * @param user      user
     * @param fileEntry file entry
     * @return success json string
     */
    @RestAPI
    @RequestMapping(value = {"/api/", "/api"}, method = RequestMethod.POST)
    public HttpEntity<JSONObject> create(User user, FileEntry fileEntry) throws IOException {
        fileEntryService.saveFile(user, fileEntry);
        return successJsonHttpEntity();
    }

    /**
     * NEW
     *
     * @param user       新增用户
     * @param path       新增路径
     * @param folderName 新增路径下的文件名称
     * @return 创建成功的文件信息
     */
    @RestAPI
    @ResponseBody
    @RequestMapping(value = "/api/new/folder", method = RequestMethod.POST)
    public ResponseEntity<FileEntry> addFolderApi(User user, @RequestParam String path,
                                                  @RequestParam("folderName") String folderName) { // "fileName"
        fileEntryService.addFolder(user, path, StringUtils.trimToEmpty(folderName));
        FileEntry folder = null;
        try {
            folder = fileEntryService.getSpecifyScript(user, path);
            return new ResponseEntity<FileEntry>(folder, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<FileEntry>(folder, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RestAPI
    @ResponseBody
    @RequestMapping(value = "/api/new/script", /**params = "type=script",**/method = RequestMethod.POST)
    public HttpEntity<String> createFormApi(User user, @RequestParam String path,
                                            @RequestParam(value = "testUrl", required = false) String testUrl,
                                            @RequestParam("fileName") String fileName,
                                            @RequestParam(value = "scriptType", required = false) String scriptType,
                                            @RequestParam(value = "createLibAndResource", defaultValue = "false") boolean createLibAndResources,
                                            @RequestParam(value = "options", required = false) String options,
                                            RedirectAttributes redirectAttributes, ModelMap model) throws IOException {
        fileName = StringUtils.trimToEmpty(fileName);
        String name = "Test1";
        if (StringUtils.isEmpty(testUrl)) {
            testUrl = StringUtils.defaultIfBlank(testUrl, "http://please_modify_this.com");
        } else {
            name = UrlUtils.getHost(testUrl);
        }
        ScriptHandler scriptHandler = fileEntryService.getScriptHandler(scriptType);
        FileEntry entry = new FileEntry();
        entry.setPath(fileName);
        if (scriptHandler instanceof ProjectHandler) {
            if (!fileEntryService.hasFileEntry(user, PathUtils.join(path, fileName))) {
                fileEntryService.prepareNewEntry(user, path, fileName, name, testUrl, scriptHandler,
                    createLibAndResources, options);
                redirectAttributes.addFlashAttribute("message", fileName + " project is created.");
                return toJsonHttpEntity(model);
            } else {
                redirectAttributes.addFlashAttribute("exception", fileName
                    + " is already existing. Please choose the different name");
                return toJsonHttpEntity(model);
            }

        } else {
            String fullPath = PathUtils.join(path, fileName);
            if (fileEntryService.hasFileEntry(user, fullPath)) {
                model.addAttribute("file", fileEntryService.getSpecifyScript(user, fullPath));
            } else {
                model.addAttribute("file", fileEntryService.prepareNewEntry(user, path, fileName, name, testUrl,
                    scriptHandler, createLibAndResources, options));
            }
        }
        model.addAttribute("breadcrumbPath", getScriptPathBreadcrumbs(PathUtils.join(path, fileName)));
        model.addAttribute("scriptHandler", scriptHandler);
        model.addAttribute("createLibAndResource", createLibAndResources);
        return toJsonHttpEntity(model);
    }

    @RestAPI
    @ResponseBody
    @RequestMapping(value = "/api/save", method = RequestMethod.POST)
    public ResponseEntity<FileEntry> saveApi(User user, FileEntry fileEntry,
                                             @RequestParam String targetHosts, @RequestParam(defaultValue = "0") String validated,
                                             @RequestParam(defaultValue = "false") boolean createLibAndResource, ModelMap model) {
        String cont = fileEntry.getContent();
        cont = cont.replaceAll("&quot;", "\"");
        cont = cont.replaceAll("&amp;", "&");
        cont = cont.replaceAll("&#39;", "\'");
        cont = cont.replaceAll("&lt;", "<");
        cont = cont.replaceAll("&gt;", ">");
        fileEntry.setContent(cont);
        if (fileEntry.getFileType().getFileCategory() == FileCategory.SCRIPT) {
            Map<String, String> map = Maps.newHashMap();
            map.put("validated", validated);
            map.put("targetHosts", StringUtils.trim(targetHosts));
            fileEntry.setProperties(map);
        }
        try {
            fileEntryService.saveFile(user, fileEntry);
        } catch (IOException e) {
            return new ResponseEntity<FileEntry>(fileEntry, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String basePath = getPath(fileEntry.getPath());
        if (createLibAndResource) {
            fileEntryService.addFolder(user, basePath, "lib");
            fileEntryService.addFolder(user, basePath, "resources");
        }
        model.clear();
        return new ResponseEntity<FileEntry>(fileEntry, HttpStatus.CREATED);
    }

    @RestAPI
    @RequestMapping(value = "/api/delete", method = RequestMethod.DELETE)
    public String deleteApi(User user, @RequestParam String path, @RequestParam("filesString") String filesString) {
        String[] files = filesString.split(",");
        fileEntryService.deleteFile(user, path, files);
        Map<String, Object> rtnMap = new HashMap<String, Object>(1);
        rtnMap.put(JSON_SUCCESS, true);
        return toJson(rtnMap);
    }

    /**
     * Create the given file.
     *
     * @param user        user
     * @param path        path
     * @param description description
     * @param file        multi part file
     * @return success json string
     */
    @RestAPI
    @RequestMapping(value = "/api/upload", method = RequestMethod.POST)
    public HttpEntity<JSONObject> uploadForAPI(User user, @RequestParam String path,
                                               @RequestParam("description") String description,
                                               @RequestParam("uploadFile") MultipartFile file) throws IOException {
        upload(user, path, file);
        return successJsonHttpEntity();
    }

    /**
     * Check the file by given path.
     *
     * @param user user
     * @param path path
     * @return json string
     */
    @RestAPI
    @RequestMapping(value = "/api/view", method = RequestMethod.GET)
    public HttpEntity<String> viewOne(User user, @RequestParam String path) throws IOException {
        FileEntry fileEntry = fileEntryService.getSpecifyScript(user, path);
        return toJsonHttpEntity(checkNotNull(fileEntry
            , "%s file is not viewable", path));
    }

    /**
     * Get all files which belongs to given user.
     *
     * @param user user
     * @return json string
     */
    @RestAPI
    @RequestMapping(value = {"/api/all"}, method = RequestMethod.GET)
    public HttpEntity<String> getAll(User user) {
        try {
            return toJsonHttpEntity(fileEntryService.getUserScriptAllFiles(user, "/"));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get all files which belongs to given user and path.
     *
     * @param user user
     * @param path path
     * @return json string
     */
    @RestAPI
    @RequestMapping(value = {"/api"}, method = RequestMethod.GET)
    public HttpEntity<String> getAll(User user, @RequestParam String path) {
        return toJsonHttpEntity(getAllFiles(user, path));
    }

    /**
     * 获取用户脚本路径下面指定子路径{@see path}的所有文件
     *
     * @param user 查询用户
     * @param path 脚本路径
     * @return 路径下所有文件
     */
    public List<FileEntry> getAllFiles(User user, String path) {
        final String trimmedPath = StringUtils.trimToEmpty(path);
        List<FileEntry> files = null;
        try {
            files = fileEntryService.getUserScriptAllFiles(user, trimmedPath);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return files;
    }

    /**
     * Delete file by given user and path.
     *
     * @param user query user
     * @param path path,the path in user script dir
     * @return json string
     */
    @RestAPI
    @RequestMapping(value = "/api", method = RequestMethod.DELETE)
    public HttpEntity<JSONObject> deleteOne(User user, @RequestParam String path) {
        fileEntryService.deleteFile(user, path);
        return successJsonHttpEntity();
    }


    /**
     * Validate the script.
     *
     * @param user       current user
     * @param fileEntry  fileEntry
     * @param hostString hostString
     * @return validation Result string
     */
    @RequestMapping(value = "/api/validate", method = RequestMethod.POST)
    @RestAPI
    public HttpEntity<String> validate(User user, FileEntry fileEntry,
                                       @RequestParam(value = "hostString", required = false) String hostString) {
        String cont = fileEntry.getContent();
        cont = cont.replaceAll("&quot;", "\"");
        cont = cont.replaceAll("&amp;", "&");
        cont = cont.replaceAll("&#39;", "\'");
        cont = cont.replaceAll("&lt;", "<");
        cont = cont.replaceAll("&gt;", ">");
        fileEntry.setContent(cont);
        fileEntry.setCreatedUser(user);
        return toJsonHttpEntity(scriptValidationService.validate(user, fileEntry, false, hostString));
    }

    @RequestMapping(value = "/api/validateScript", method = RequestMethod.POST)
    public HttpEntity<String> validateScript(User user, @RequestParam String jsonObject,
                                             @RequestParam(value = "hostString", required = false) String hostString) {
        FileEntry fileEntry = JSON.parseObject(jsonObject, FileEntry.class);
        return validate(user, fileEntry, hostString);
    }
}
