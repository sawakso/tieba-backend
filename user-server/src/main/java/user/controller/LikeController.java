package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.service.ILikeService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/likes")
@Api(tags = "点赞接口")
public class LikeController {

    @Resource
    private ILikeService likeService;

    @ApiOperation("点赞/取消点赞帖子")
    @PostMapping("/post")
    public Result likePost(@RequestParam Long postId) {
        return likeService.like(1, postId); // target_type 1:帖子
    }

    @ApiOperation("点赞评论")
    @PostMapping("/comment")
    public Result likeComment(@RequestParam Long commentId) {
        return likeService.like(2, commentId); // target_type 2:回复
    }


    @ApiOperation("检查用户是否点赞帖子")
    @GetMapping("/post/check")
    public Result checkPostLike(@RequestParam Long userId,
                                @RequestParam Long postId) {
        return likeService.checkLike(userId, 1, postId);
    }

    @ApiOperation("检查用户是否点赞评论")
    @GetMapping("/comment/check")
    public Result checkCommentLike(@RequestParam Long userId,
                                   @RequestParam Long commentId) {
        return likeService.checkLike(userId, 2, commentId);
    }

    @ApiOperation("获取用户点赞的帖子列表")
    @GetMapping("/user/{userId}/posts")
    public Result getUserLikedPosts(@PathVariable Long userId,
                                    @RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return likeService.getUserLikedTargets(userId, 1, page, size);
    }

    @ApiOperation("获取用户点赞的评论列表")
    @GetMapping("/user/{userId}/comments")
    public Result getUserLikedComments(@PathVariable Long userId,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        return likeService.getUserLikedTargets(userId, 2, page, size);
    }
}