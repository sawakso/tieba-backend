package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.domain.dto.Result;
import user.service.IPostService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/posts")
@Api(tags = "管理员-帖子管理")
public class AdminPostController {

    @Resource
    private IPostService postService;

    @ApiOperation("更新帖子统计信息")
    @PutMapping("/statistics/{postId}")
    public Result updateStatistics(@PathVariable Long postId) {
        return postService.updatePostStatistics(postId);
    }

    @ApiOperation("批量更新所有帖子统计")
    @PutMapping("/statistics/all")
    public Result updateAllStatistics() {
        return postService.updateAllPostsStatistics();
    }
}