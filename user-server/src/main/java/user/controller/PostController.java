package user.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.domain.po.Bars;
import user.domain.po.Comment;
import user.domain.po.Post;
import user.domain.po.User;
import user.domain.vo.PostVO;
import user.service.IBarsService;
import user.service.ICommentService;
import user.service.IPostService;
import user.service.IUserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

//个人帖子相关
@Api(tags = "帖子接口")
@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private IPostService postService;
    @Resource
    private IUserService userService;
    @Resource
    private ICommentService commentService;
    @Resource
    private IBarsService barsService;

    @ApiOperation("查询个人帖子")
    @GetMapping("/queryMyPosts")
    public Result queryMyPosts(Long id, HttpServletRequest request) {
        return postService.queryMyPosts(id, request);
    }

    @ApiOperation("删除个人帖子")
    @DeleteMapping("/deleteMyPost")
    public Result deleteMyPost(@RequestParam Long id) {
        boolean removed = postService.removeById(id);
        return removed ? Result.ok() : Result.fail("删除失败");
    }

    @ApiOperation("修改个人帖子")
    @PostMapping("/updateMyPost")
    public Result updateMyPost(@RequestBody Post post) {
        boolean updated = postService.updateById(post);
        return updated ? Result.ok() : Result.fail("修改失败");
    }

    @ApiOperation("添加个人帖子")
    @PostMapping("/addMyPost")
    public Result addMyPost(@RequestBody Post post) {
        boolean saved = postService.save(post);
        return saved ? Result.ok() : Result.fail("添加失败");
    }

    @ApiOperation("查询帖子详情")
    @GetMapping("/queryPostDetail")
    public Result queryPostDetail(@RequestParam Long id) {
        // 获取帖子数据
        Post post = postService.getById(id);
        User user = userService.getById(post.getUserId());
        if (post != null) {
            // 确保返回的是普通对象，而非代理对象
            // 将 post 转换为一个普通的 Map 或者直接返回对象
            // 这里假设 Post 是一个简单的 Java Bean，你可以手动设置它的属性
            Map<String, Object> postDetails = new HashMap<>();
            postDetails.put("id", post.getId());
            postDetails.put("title", post.getTitle());
            postDetails.put("content", post.getContent());
            postDetails.put("userId", post.getUserId());
            postDetails.put("userName", user != null ? user.getUsername() : "匿名");            postDetails.put("viewCount", post.getViewCount());
            postDetails.put("likeCount", post.getLikeCount());
            postDetails.put("replyCount", post.getReplyCount());
            postDetails.put("createTime", post.getCreateTime());
            postDetails.put("updateTime", post.getUpdateTime());
            postDetails.put("status", post.getStatus());

            // 返回标准的成功响应
            return Result.ok(postDetails);
        } else {
            // 如果找不到帖子，则返回失败
            return Result.fail("帖子不存在");
        }
    }

    @ApiOperation("查询帖子列表")
    @GetMapping("/queryPostList")
    public Result queryPostList() {
        return postService.getPostList();
    }

    @ApiOperation("查询作者信息")
    @GetMapping("/queryAuthorInfo")
    public Result queryAuthorInfo(@RequestParam Long userId) {
        // 假设你有一个 UserService
        User author = userService.getById(userId);
        return author != null ? Result.ok(author) : Result.fail("作者不存在");
    }

    @ApiOperation("查询评论列表")
    @GetMapping("/queryComments")
    public Result queryComments(@RequestParam Long postId,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {

        return commentService.queryByPostId(postId, page, size);
    }
    @ApiOperation("查询热门推荐帖子")
    @GetMapping("/queryHotPosts")
    public Result queryHotPosts(@RequestParam(defaultValue = "5") Integer limit) {
        List<Post> hotPosts = postService.listHotPosts(limit);
        return Result.ok(hotPosts, (long) hotPosts.size());
    }
    @ApiOperation("随机获取帖子")
    @GetMapping("/random")
    public Result random(@RequestParam(defaultValue = "10") Integer size, HttpServletRequest request) {
        return postService.random(size, request);
    }
    @ApiOperation("根据贴吧分页查询帖子")
    @GetMapping("/queryBarsPost")
    public Result queryBarsPost(
            @RequestParam Long barsId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "new") String sort) {

        // 使用 MyBatis-Plus 的分页
        Page<Post> pageParam = new Page<>(page, size);

        // 构建查询条件
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getBarId, barsId);

        // 排序逻辑
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Post::getViewCount);  // 按浏览量排序
            wrapper.orderByDesc(Post::getLikeCount);  // 第二排序：点赞数
        } else {
            wrapper.orderByDesc(Post::getCreateTime); // 按创建时间排序
        }

        // 执行分页查询
        Page<Post> pageResult = postService.page(pageParam, wrapper);

        // 返回分页结果
        return Result.ok(pageResult.getRecords(), pageResult.getTotal());
    }

}
