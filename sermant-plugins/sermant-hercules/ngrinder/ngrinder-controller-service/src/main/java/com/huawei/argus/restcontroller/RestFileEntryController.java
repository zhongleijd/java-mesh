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

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.util.HttpContainerContext;
import org.ngrinder.model.User;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.service.NfsFileEntryService;
import org.ngrinder.script.service.ScriptValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.ngrinder.common.util.ExceptionUtils.processException;

@Api(tags = "Script管理")
@RestController
@RequestMapping("/rest/api")
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
    @ApiOperation(tags = "Script管理", httpMethod = "GET", value = "查询所有Script")
    @RequestMapping(value = {"/scripts"}, method = RequestMethod.GET)
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
     * Add a folder on the given path.
     *
     * @param user       current user
     * @param path       path in which folder will be added
     * @param folderName folderName
     * @return redirect:/script/${user}/${path}
     */
    @ApiOperation(tags = "Script管理", httpMethod = "POST", value = "添加文件夹")
    @RequestMapping(value = "/folder", method = RequestMethod.POST)
    public Map<String, Object> addFolder(User user, @RequestParam("path") String path, @RequestParam("folderName") String folderName) { // "fileName"
        try {
            fileEntryService.addFolder(user, StringUtils.trimToEmpty(path), StringUtils.trimToEmpty(folderName));
        } catch (IOException e) {
            return fail();
        }
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
    @ApiOperation(tags = "Script管理", httpMethod = "POST", value = "创建一个Script")
    @RequestMapping(value = "/script", method = RequestMethod.POST)
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
    @ApiOperation(tags = "Script管理", httpMethod = "GET", value = "查询指定路径脚本信息")
    @RequestMapping(value = "/script", method = RequestMethod.GET)
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
    @ApiOperation(tags = "Script管理", httpMethod = "GET", value = "下载指定Script")
    @RequestMapping("/download/script")
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
     * 判断脚本是否存在
     *
     * @param user 当前用户
     * @param path 脚本全路径
     * @return 判断结果
     */
    @ApiOperation(tags = "Script管理", httpMethod = "GET", value = "判断脚本是否存在")
    @RequestMapping(value = "/exist/script", method = RequestMethod.GET)
    public Map<String, Object> hasScript(User user, @RequestParam String path) {
        if (StringUtils.isEmpty(path)) {
            return fail();
        }
        try {
            if (fileEntryService.hasFileEntry(user, path)) {
                return success();
            }
            return fail();
        } catch (IOException e) {
            return fail();
        }
    }

    /**
     * Upload a file.
     *
     * @param user current user
     * @param path path
     * @param file multi part file
     * @return redirect:/script/list/${path}
     */
    @ApiOperation(tags = "Script管理", httpMethod = "POST", value = "指定路径上传Script")
    @RequestMapping(value = "/upload/script", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFile(User user, @RequestParam String path, @RequestPart("uploadFile") MultipartFile file) {
        try {
            upload(user, path, file);
            return success();
        } catch (IOException e) {
            LOG.error("Error while getting file content: {}", e.getMessage(), e);
            return fail();
        }
    }

    private void upload(User user, String path, MultipartFile file) throws IOException {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setContentBytes(file.getBytes());
        fileEntry.setPath(FilenameUtils.separatorsToUnix(FilenameUtils.concat(path, file.getOriginalFilename())));
        fileEntryService.saveFile(user, fileEntry);
    }

    @ApiOperation(tags = "Script管理", httpMethod = "DELETE", value = "批量删除指定脚本")
    @RequestMapping(value = "/script", method = RequestMethod.DELETE)
    public Map<String, Object> deleteApi(User user, @RequestParam String path, @RequestParam("files") String filesString) {
        String[] fileNames = filesString.split(",");
        try {
            fileEntryService.deleteFile(user, path, fileNames);
        } catch (IOException e) {
            return fail();
        }
        return success();
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
        List<FileEntry> files;
        try {
            files = fileEntryService.getUserScriptAllFiles(user, trimmedPath);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return files;
    }

    /**
     * Validate the script.
     *
     * @param user       current user
     * @param fileEntry  fileEntry
     * @param hostString hostString
     * @return validation Result string
     */
    @ApiOperation(tags = "Script管理", httpMethod = "POST", value = "判断脚本是否有效")
    @RequestMapping(value = "/validate/script", method = RequestMethod.POST)
    public HttpEntity<String> validate(User user, FileEntry fileEntry,
                                       @RequestParam(value = "hostString", required = false) String hostString) {
        String cont = fileEntry.getContent();
        fileEntry.setContent(cont);
        fileEntry.setCreatedUser(user);
        return toJsonHttpEntity(scriptValidationService.validate(user, fileEntry, false, hostString));
    }
}
