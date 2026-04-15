package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.domain.po.Comment;
import user.service.ICommentService;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "评论接口")
@RestController
@RequestMapping("/post/comment")
public class CommentController {

    @Autowired
    private ICommentService commentService;

    @ApiOperation("添加评论")
    @PostMapping("/add")
    public Result add(@RequestBody Comment comment) {

        return commentService.addComment(comment);
    }

    @ApiOperation("查询帖子评论（含楼中楼）")
    @GetMapping("/listByPost")
    public Result list(@RequestParam Long postId) {

        return commentService.getCommentsTree(postId);
    }

    @ApiOperation("删除评论及其子评论")
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Long commentId) {

        return commentService.removeWithChildren(commentId);
    }
    @ApiOperation("查询当前登录用户评论（分页）")
    @GetMapping("/user")
    public Result queryMyComments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return commentService.queryMyComments(request, page, size);
    }
}