package user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.domain.dto.Result;
import user.domain.dto.UserDTO;
import user.domain.po.Comment;
import user.domain.po.Post;
import user.domain.po.User;
import user.domain.vo.CommentVO;
import user.mapper.CommentMapper;
import user.service.ICommentService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import user.service.IPostService;
import user.service.IUserService;
import user.utils.UserHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static user.utils.RdeisConstants.LOGIN_USER_KEY;

/**
 * 评论服务实现类
 * @author tieba
 */
@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements ICommentService {
    @Resource
    private IUserService userService;
    @Resource
    private IPostService postService;
    @Resource
    private StringRedisTemplate redis;

    @ApiOperation("添加评论")
    @Override
    @Transactional(rollbackFor = Exception.class)  // 添加事务，保证数据一致性
    public Result addComment(Comment comment) {
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("未登录");
        }

        // 设置评论用户ID
        comment.setUserId(user.getId());

        // 设置楼层
        Integer maxFloor = lambdaQuery()
                .eq(Comment::getPostId, comment.getPostId())
                .orderByDesc(Comment::getFloor)
                .last("limit 1")
                .oneOpt()
                .map(Comment::getFloor)
                .orElse(0);

        comment.setFloor(maxFloor + 1);
        comment.setStatus(1);

        // 保存评论
        boolean saved = save(comment);

        if (saved) {
            // 更新帖子的回复数 +1
            boolean updated = postService.lambdaUpdate()
                    .eq(Post::getId, comment.getPostId())
                    .setSql("reply_count = reply_count + 1")
                    .update();

            if (updated) {
                log.info("评论添加成功: postId={}, userId={}, floor={}",
                        comment.getPostId(), comment.getUserId(), comment.getFloor());
                return Result.ok("评论成功");
            } else {
                log.warn("评论保存成功但更新帖子回复数失败: postId={}", comment.getPostId());
                // 这里可以选择回滚或返回警告
                return Result.ok("评论成功，但更新帖子统计失败");
            }
        } else {
            log.error("评论保存失败: postId={}, userId={}", comment.getPostId(), comment.getUserId());
            return Result.fail("发布失败");
        }
    }
    @Override
    public Result getCommentsTree(Long postId) {
        // 查询所有有效评论（按楼层升序）
        List<Comment> list = lambdaQuery()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getFloor)  // 按楼层排序
                .list();

        if (list == null || list.isEmpty()) {
            return Result.ok(new ArrayList<>(), 0L);
        }

        // 转换为 CommentVO 并填充用户名（不再构建树形结构）
        List<CommentVO> commentVOs = new ArrayList<>();
        for (Comment c : list) {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setFloor(c.getFloor());
            vo.setLikeCount(c.getLikeCount());
            vo.setStatus(c.getStatus());
            vo.setReplyToUserId(c.getReplyToUserId());
            vo.setReplyToFloor(c.getReplyToFloor());
            vo.setCreateTime(c.getCreateTime());
            vo.setUpdateTime(c.getUpdateTime());

            // 获取用户名
            User user = userService.getById(c.getUserId());
            vo.setUsername(user != null ? user.getUsername() : "未知用户");

            // 注意：不再设置 children 字段

            commentVOs.add(vo);
        }

        // 直接返回平铺列表
        return Result.ok(commentVOs, (long) commentVOs.size());
    }

    @Override
    public Result removeWithChildren(Long commentId) {
        Comment root = getById(commentId);
        if (root == null) return Result.fail("不存在");

        lambdaUpdate()
                .eq(Comment::getPostId, root.getPostId())
                .eq(Comment::getFloor, root.getFloor())
                .or()
                .eq(Comment::getReplyToFloor, root.getFloor())
                .set(Comment::getStatus, 0)
                .update();

        return Result.ok();
    }

    @Override
    public Result countByPostId(Long postId) {
        long count = lambdaQuery()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .count();
        return Result.ok(count);
    }

    @Override
    public Result queryByPostId(Long postId, Integer page, Integer size) {
        Page<Comment> p = new Page<>(page, size);
        Page<Comment> res = lambdaQuery()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getFloor)
                .page(p);

        return Result.ok(res.getRecords(), res.getTotal());
    }
    @Override
    public Result queryMyComments(HttpServletRequest request, Integer page, Integer size) {

        // 1. 获取当前用户
        Long userId = null;
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String key = LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = redis.opsForHash().entries(key);
            if (!userMap.isEmpty()) {
                userId = Long.valueOf(userMap.get("id").toString());
            }
        }

        if (userId == null) {
            return Result.fail("未登录");
        }

        // 2. 分页查询评论
        Page<Comment> p = new Page<>(page, size);
        Page<Comment> resultPage = lambdaQuery()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime)
                .page(p);

        List<Comment> comments = resultPage.getRecords();

        if (comments.isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }

        // 3. 收集需要查询的关联ID
        Set<Long> postIds = comments.stream()
                .map(Comment::getPostId)
                .collect(Collectors.toSet());

        Set<Long> replyToUserIds = comments.stream()
                .map(Comment::getReplyToUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. 批量查询帖子信息
        Map<Long, Post> postMap = postService.lambdaQuery()
                .in(Post::getId, postIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        // 5. 批量查询被回复的用户信息
        Map<Long, User> replyUserMap = Collections.emptyMap();
        if (!replyToUserIds.isEmpty()) {
            replyUserMap = userService.lambdaQuery()
                    .in(User::getId, replyToUserIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }

        // 6. 批量查询被回复的评论内容
        // 先收集需要查询的 key (postId_floor)
        Map<String, String> replyContentMap = new HashMap<>();
        for (Comment comment : comments) {
            if (comment.getReplyToFloor() != null && comment.getPostId() != null) {
                String key = comment.getPostId() + "_" + comment.getReplyToFloor();
                replyContentMap.put(key, null);
            }
        }

        // 查询数据库获取被回复的评论内容
        if (!replyContentMap.isEmpty()) {
            // 提取所有需要查询的 postId
            Set<Long> targetPostIds = comments.stream()
                    .filter(c -> c.getReplyToFloor() != null)
                    .map(Comment::getPostId)
                    .collect(Collectors.toSet());

            if (!targetPostIds.isEmpty()) {
                List<Comment> replyComments = lambdaQuery()
                        .select(Comment::getPostId, Comment::getFloor, Comment::getContent)
                        .in(Comment::getPostId, targetPostIds)
                        .eq(Comment::getStatus, 1)
                        .list();

                for (Comment c : replyComments) {
                    String key = c.getPostId() + "_" + c.getFloor();
                    replyContentMap.put(key, c.getContent());
                }
            }
        }

        // 7. 组装 CommentVO（lambda 中只读取，不修改外部变量）
        final Map<Long, Post> finalPostMap = postMap;
        final Map<Long, User> finalReplyUserMap = replyUserMap;
        final Map<String, String> finalReplyContentMap = replyContentMap;

        List<CommentVO> voList = comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(comment, vo);

            // 设置帖子标题
            Post post = finalPostMap.get(comment.getPostId());
            if (post != null) {
                vo.setPostTitle(post.getTitle());
            }

            // 设置被回复的用户名
            if (comment.getReplyToUserId() != null) {
                User replyUser = finalReplyUserMap.get(comment.getReplyToUserId());
                if (replyUser != null) {
                    vo.setReplyToUserName(replyUser.getUsername());
                }
            }

            // 设置被回复的评论内容
            if (comment.getReplyToFloor() != null && comment.getPostId() != null) {
                String key = comment.getPostId() + "_" + comment.getReplyToFloor();
                String replyContent = finalReplyContentMap.get(key);
                if (replyContent != null) {
                    // 截取前50个字符，避免太长
                    if (replyContent.length() > 50) {
                        vo.setReplyToContent(replyContent.substring(0, 50) + "...");
                    } else {
                        vo.setReplyToContent(replyContent);
                    }
                }
            }

            return vo;
        }).collect(Collectors.toList());

        // 8. 返回结果
        return Result.ok(voList, resultPage.getTotal());
    }
    @Override
    public Comment getCommentById(Long id) {
        if (id == null) {
            return null;
        }
        return this.getById(id);
    }

    @Override
    public List<Comment> getCommentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids);
        return this.list(wrapper);
    }
}