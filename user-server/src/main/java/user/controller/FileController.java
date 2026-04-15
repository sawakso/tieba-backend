package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import user.domain.dto.Result;
import user.service.IFileService;

@RestController
@RequestMapping("/file")
@Api(tags = "文件管理")
public class FileController {

    @Autowired
    private IFileService fileService;

    @ApiOperation("上传文件（图片/视频）")
    @PostMapping("/upload")
    public Result upload(
            @ApiParam(value = "文件", required = true)
            @RequestParam("file") MultipartFile file,

            @ApiParam(value = "业务类型（post/comment/avatar/background）", required = true)
            @RequestParam("bizType") String bizType,

            @ApiParam(value = "业务ID（帖子/评论ID，可为空）")
            @RequestParam(value = "bizId", required = false) Long bizId
    ) {
        String key = fileService.upload(file, bizType, bizId);
        return Result.ok(key);
    }

    @ApiOperation("根据业务查询文件")
    @GetMapping("/list")
    public Result list(
            @ApiParam(value = "业务类型", required = true)
            @RequestParam String bizType,

            @ApiParam(value = "业务ID", required = true)
            @RequestParam Long bizId
    ) {
        return Result.ok(fileService.listByBiz(bizType, bizId));
    }
}