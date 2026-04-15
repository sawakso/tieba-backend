package user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.domain.dto.Result;
import user.domain.po.Comment;
import user.domain.po.Like;
import user.domain.po.Post;
import user.mapper.LikeMapper;
import user.service.ICommentService;
import user.service.ILikeService;
import user.service.IPostService;
import user.utils.UserHolder;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like>
        implements ILikeService {

    @Resource
    private IPostService postService;

    @Resource
    private ICommentService commentService;

    @Resource
    private StringRedisTemplate redis;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result like(Integer targetType, Long targetId) {

        Long userId = UserHolder.getUser().getId();
        String type = (targetType == 1) ? "post" : "comment";
        String key = "tieba:liked:" + type + ":" + targetId;

        Boolean isLike = redis.opsForSet().isMember(key, userId.toString());

        if (isLike == null || !isLike) {
            // 👍 点赞

            // 1. 插入点赞记录
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            this.save(like);

            // 2. 更新点赞数
            boolean success;
            if (targetType == 1) {
                success = postService.update(
                        new LambdaUpdateWrapper<Post>()
                                .setSql("like_count = IFNULL(like_count,0) + 1")
                                .eq(Post::getId, targetId)
                );
            } else if (targetType == 2) {
                success = commentService.update(
                        new LambdaUpdateWrapper<Comment>()
                                .setSql("like_count = IFNULL(like_count,0) + 1")
                                .eq(Comment::getId, targetId)
                );
            } else {
                throw new RuntimeException("类型错误");
            }

            if (!success) throw new RuntimeException("点赞失败");

            // 3. Redis
            redis.opsForSet().add(key, userId.toString());

        } else {
            // 👎 取消点赞

            // 1. 删除点赞记录
            this.remove(
                    new LambdaQueryWrapper<Like>()
                            .eq(Like::getUserId, userId)
                            .eq(Like::getTargetType, targetType)
                            .eq(Like::getTargetId, targetId)
            );

            // 2. 更新点赞数
            boolean success;
            if (targetType == 1) {
                success = postService.update(
                        new LambdaUpdateWrapper<Post>()
                                .setSql("like_count = IFNULL(like_count,0) - 1")
                                .eq(Post::getId, targetId)
                );
            } else if (targetType == 2) {
                success = commentService.update(
                        new LambdaUpdateWrapper<Comment>()
                                .setSql("like_count = IFNULL(like_count,0) - 1")
                                .eq(Comment::getId, targetId)
                );
            } else {
                throw new RuntimeException("类型错误");
            }

            if (!success) throw new RuntimeException("取消点赞失败");

            // 3. Redis
            redis.opsForSet().remove(key, userId.toString());
        }

        // ========== 新增：返回最新数据 ==========
        Map<String, Object> data = new HashMap<>();

        // 当前点赞状态
        data.put("isLiked", !(isLike == null ? false : isLike)); // 取反，因为前面已经操作过了

        // 查询最新点赞数
        Integer likeCount;
        if (targetType == 1) {
            likeCount = postService.getObj(
                    new LambdaQueryWrapper<Post>()
                            .select(Post::getLikeCount)
                            .eq(Post::getId, targetId),
                    obj -> obj == null ? 0 : (Integer) obj
            );
        } else {
            likeCount = commentService.getObj(
                    new LambdaQueryWrapper<Comment>()
                            .select(Comment::getLikeCount)
                            .eq(Comment::getId, targetId),
                    obj -> obj == null ? 0 : (Integer) obj
            );
        }
        data.put("likeCount", likeCount);

        return Result.ok(data);
    }



    @Override
    public Result checkLike(Long userId, Integer targetType, Long targetId) {
        try {
            if (userId == null || userId <= 0) {
                return Result.fail("用户ID不能为空");
            }
            if (targetType == null || (targetType != 1 && targetType != 2)) {
                return Result.fail("点赞类型错误");
            }
            if (targetId == null || targetId <= 0) {
                return Result.fail("目标ID不能为空");
            }

            LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, targetType)
                    .eq(Like::getTargetId, targetId);

            long count = this.count(wrapper);
            boolean isLiked = count > 0;

            Map<String, Object> data = new HashMap<>();
            data.put("isLiked", isLiked);
            data.put("userId", userId);
            data.put("targetType", targetType);
            data.put("targetId", targetId);

            return Result.ok(data);

        } catch (Exception e) {
            log.error("检查点赞状态失败: userId={}, targetType={}, targetId={}", userId, targetType, targetId, e);
            return Result.fail("检查失败：" + e.getMessage());
        }
    }

    @Override
    public Result getUserLikedTargets(Long userId, Integer targetType, Integer page, Integer size) {
        try {
            if (userId == null || userId <= 0) {
                return Result.fail("用户ID不能为空");
            }
            if (targetType == null || (targetType != 1 && targetType != 2)) {
                return Result.fail("点赞类型错误");
            }

            // 处理分页参数默认值（现在 page 和 size 是 Integer 类型）
            if (page == null || page < 1) {
                page = 1;
            }
            if (size == null || size < 1 || size > 100) {
                size = 10;
            }

            // MyBatis-Plus 的 Page 构造器接受 long 类型，Integer 会自动转为 long
            Page<Like> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, targetType)
                    .orderByDesc(Like::getCreateTime);

            Page<Like> likePage = this.page(pageParam, wrapper);

            List<Like> likeList = likePage.getRecords();

            if (likeList.isEmpty()) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("total", 0L);
                emptyResult.put("pages", 0);
                emptyResult.put("current", page);
                emptyResult.put("size", size);
                emptyResult.put("records", new ArrayList<>());
                return Result.ok(emptyResult);
            }

            // targetId 已经是 Long 类型，不需要转换
            List<Long> targetIds = likeList.stream()
                    .map(Like::getTargetId)
                    .collect(Collectors.toList());

            Object records = null;

            if (targetType == 1) {
                // 需要确保 postService.getPostsByIds 接受 List<Long>
                List<Post> posts = postService.getPostsByIds(targetIds);

                List<Map<String, Object>> postRecords = new ArrayList<>();
                for (Post post : posts) {
                    Like like = likeList.stream()
                            .filter(l -> l.getTargetId().equals(post.getId()))
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", post.getTitle());
                    postMap.put("content", post.getContent());
                    postMap.put("userId", post.getUserId());
                    postMap.put("replyCount", post.getReplyCount());
                    postMap.put("likeCount", post.getLikeCount());
                    postMap.put("createTime", post.getCreateTime());
                    postMap.put("likeTime", like != null ? like.getCreateTime() : null);
                    postRecords.add(postMap);
                }
                records = postRecords;

            } else if (targetType == 2) {
                // 需要确保 commentService.getCommentsByIds 接受 List<Long>
                List<Comment> comments = commentService.getCommentsByIds(targetIds);

                List<Map<String, Object>> commentRecords = new ArrayList<>();
                for (Comment comment : comments) {
                    Like like = likeList.stream()
                            .filter(l -> l.getTargetId().equals(comment.getId()))
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> commentMap = new HashMap<>();
                    commentMap.put("id", comment.getId());
                    commentMap.put("content", comment.getContent());
                    commentMap.put("postId", comment.getPostId());
                    commentMap.put("userId", comment.getUserId());
                    commentMap.put("floor", comment.getFloor());
                    commentMap.put("likeCount", comment.getLikeCount() != null ? comment.getLikeCount() : 0);
                    commentMap.put("replyCount", 0);
                    commentMap.put("createTime", comment.getCreateTime());
                    commentMap.put("likeTime", like != null ? like.getCreateTime() : null);

                    // 获取帖子标题
                    try {
                        // 需要确保 postService.getPostById 接受 Long 类型
                        Post post = postService.getPostById(comment.getPostId());
                        commentMap.put("postTitle", post != null ? post.getTitle() : "未知帖子");
                    } catch (Exception e) {
                        commentMap.put("postTitle", "帖子已删除");
                    }

                    commentRecords.add(commentMap);
                }
                records = commentRecords;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", likePage.getTotal());
            result.put("pages", likePage.getPages());
            result.put("current", likePage.getCurrent());
            result.put("size", likePage.getSize());
            result.put("records", records);

            return Result.ok(result);

        } catch (Exception e) {
            log.error("获取用户点赞列表失败: userId={}, targetType={}", userId, targetType, e);
            return Result.fail("获取失败：" + e.getMessage());
        }
    }

    private void updateTargetLikeCount(Integer targetType, Long targetId, int delta) {
        if (targetType == 1) {
            // 需要确保 postService.getPostById 接受 Long 类型
            Post post = postService.getPostById(targetId);
            if (post != null) {
                post.setLikeCount((post.getLikeCount() != null ? post.getLikeCount() : 0) + delta);
                postService.updateById(post);
                log.info("更新帖子点赞数: postId={}, newLikeCount={}", targetId, post.getLikeCount());
            }
        } else if (targetType == 2) {
            // 需要确保 commentService.getCommentById 接受 Long 类型
            Comment comment = commentService.getCommentById(targetId);
            if (comment != null) {
                Integer currentLikeCount = comment.getLikeCount() != null ? comment.getLikeCount() : 0;
                comment.setLikeCount(currentLikeCount + delta);
                commentService.updateById(comment);
                log.info("更新评论点赞数: commentId={}, newLikeCount={}", targetId, comment.getLikeCount());
            }
        }
    }
}