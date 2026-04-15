package user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.domain.dto.Result;
import user.domain.po.*;
import user.domain.vo.PostVO;
import user.mapper.PostMapper;
import user.service.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;  // ✅ 正确的包
import user.utils.UserHolder;

import static user.utils.RdeisConstants.LOGIN_USER_KEY;

@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements IPostService {
    @Resource
    @Lazy
    public ILikeService likeService;
    @Resource
    @Lazy
    public ICommentService commentService;
    @Resource
    @Lazy
    private IBarsService barsService;  // 注入 BarsService
    @Resource
    @Lazy
    private IPostService postService;
    @Resource
    @Lazy
    private IUserService userService;
    @Resource
    @Lazy
    private StringRedisTemplate redis;

    @Override
    public Result queryMyPosts(Long userId, HttpServletRequest request) {

        // 1. 查帖子
        List<Post> posts = lambdaQuery()
                .eq(Post::getUserId, userId)
                .list();

        if (posts.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 2. 获取当前登录用户
        Long currentUserId = null;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String key = LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = redis.opsForHash().entries(key);
            if (!userMap.isEmpty()) {
                currentUserId = Long.valueOf(userMap.get("id").toString());
            }
        }

        // 3. 查点赞状态
        Map<Long, Boolean> likedMap = new HashMap<>();
        if (currentUserId != null) {
            for (Post post : posts) {
                String key = "tieba:liked:post:" + post.getId();
                Boolean liked = redis.opsForSet()
                        .isMember(key, String.valueOf(currentUserId));
                likedMap.put(post.getId(), Boolean.TRUE.equals(liked));
            }
        }

        // 4. 转 VO
        List<PostVO> list = posts.stream().map(p -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(p, vo);
            vo.setIsLiked(likedMap.getOrDefault(p.getId(), false));
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(list, (long) list.size());
    }

    @Override
    public List<Post> listHotPosts(Integer limit) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("likes");
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return this.list(wrapper);
    }

    @Override
    public List<Post> getPostsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids);
        return this.list(wrapper);
    }

    @Override
    public Post getPostById(Long id) {
        if (id == null) {
            return null;
        }
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updatePostStatistics(Long postId) {
        // 1. 参数校验
        if (postId == null || postId <= 0) {
            return Result.fail("帖子ID不能为空");
        }

        try {
            // 2. 检查帖子是否存在
            Post post = this.getById(postId);
            if (post == null) {
                return Result.fail("帖子不存在");
            }

            // 3. 统计有效评论数（status = 1 表示正常）
            Long replyCount = commentService.lambdaQuery()
                    .eq(Comment::getPostId, postId)
                    .eq(Comment::getStatus, 1)
                    .count();

            // 4. 统计点赞数（targetType = 1 表示帖子）
            Long likeCount = likeService.lambdaQuery()
                    .eq(Like::getTargetType, 1)
                    .eq(Like::getTargetId, postId)
                    .count();

            // 5. 获取最后回复信息
            Comment lastComment = commentService.lambdaQuery()
                    .eq(Comment::getPostId, postId)
                    .eq(Comment::getStatus, 1)
                    .orderByDesc(Comment::getCreateTime)
                    .last("limit 1")
                    .oneOpt()
                    .orElse(null);

            // 6. 更新帖子统计信息
            post.setReplyCount(replyCount.intValue());
            post.setLikeCount(likeCount.intValue());

            if (lastComment != null) {
                post.setLastReplyTime(lastComment.getCreateTime());
                post.setLastReplyUserId(lastComment.getUserId());
            } else {
                post.setLastReplyTime(null);
                post.setLastReplyUserId(null);
            }

            // 7. 保存更新
            boolean updated = this.updateById(post);

            if (updated) {
                log.info("更新帖子统计成功: postId={}, replyCount={}, likeCount={}",
                        postId, replyCount, likeCount);
                return Result.ok("更新成功");
            } else {
                return Result.fail("更新失败");
            }

        } catch (Exception e) {
            log.error("更新帖子统计失败: postId={}", postId, e);
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result updateAllPostsStatistics() {
        try {
            // 1. 查询所有帖子ID
            List<Long> allPostIds = this.lambdaQuery()
                    .select(Post::getId)
                    .list()
                    .stream()
                    .map(Post::getId)
                    .collect(Collectors.toList());

            if (allPostIds.isEmpty()) {
                return Result.fail("没有帖子数据");
            }

            log.info("开始更新所有帖子统计，总数：{}", allPostIds.size());

            // 2. 统计成功和失败数量
            int successCount = 0;
            int failCount = 0;

            // 3. 逐个更新
            for (Long postId : allPostIds) {
                Result result = updatePostStatistics(postId);
                // 使用 getSuccess() 方法判断（Lombok @Data 会生成）
                if (result.getSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("更新帖子统计失败: postId={}, 原因={}", postId, result.getErrorMsg());
                }
            }

            log.info("批量更新帖子统计完成: 成功={}, 失败={}", successCount, failCount);
            return Result.ok(String.format("批量更新完成，成功：%d，失败：%d", successCount, failCount));

        } catch (Exception e) {
            log.error("批量更新所有帖子统计失败", e);
            return Result.fail("批量更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result getPostList() {
        // 1. 获取当前登录用户
        Long userId = null;
        try {
            userId = UserHolder.getUser().getId();
        } catch (Exception e) {
            // 未登录用户，跳过点赞状态查询
        }

        // 2. 获取原始帖子列表
        List<Post> posts = this.list();

        if (CollectionUtils.isEmpty(posts)) {
            return Result.ok(Collections.emptyList());
        }

        // 3. 收集所有 barId
        List<Long> barIds = posts.stream()
                .map(Post::getBarId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 批量查询吧信息
        Map<Long, String> barNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(barIds)) {
            List<Bars> barsList = barsService.listByIds(barIds);
            barNameMap = barsList.stream()
                    .collect(Collectors.toMap(Bars::getId, Bars::getName));
        }

        // ========== 5. 批量查询当前用户的点赞状态 ==========
        Map<Long, Boolean> likedMap = new HashMap<>();
        if (userId != null) {
            List<Long> postIds = posts.stream()
                    .map(Post::getId)
                    .collect(Collectors.toList());

            for (Long postId : postIds) {
                String key = "tieba:liked:post:" + postId;
                Boolean isLiked = redis.opsForSet().isMember(key, userId.toString());
                likedMap.put(postId, isLiked != null && isLiked);
            }
        }

        // 6. 转换为 PostVO
        Map<Long, String> finalBarNameMap = barNameMap;
        List<PostVO> postVOList = posts.stream().map(post -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(post, vo);
            vo.setBarName(finalBarNameMap.get(post.getBarId()));

            // ⭐ 设置点赞状态
            vo.setIsLiked(likedMap.getOrDefault(post.getId(), false));

            return vo;
        }).collect(Collectors.toList());

        return Result.ok(postVOList);
    }
    @Override
    public Result random(Integer size, HttpServletRequest request) {

        // 1. 手动解析用户
        Long userId = null;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String key = LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = redis.opsForHash().entries(key);
            if (userMap != null && !userMap.isEmpty()) {
                Object idObj = userMap.get("id");
                if (idObj != null) {
                    userId = Long.valueOf(idObj.toString());
                }
            }
        }

        // 2. 随机分页
        long count = postService.count();
        if (count <= 0) return Result.ok(Collections.emptyList());

        long max = Math.max(count - size, 0);
        long offset = ThreadLocalRandom.current().nextLong(max + 1);
        long current = (offset / size) + 1;

        Page<Post> page = new Page<>(current, size);
        List<Post> posts = postService.page(page).getRecords();
        if (posts.isEmpty()) return Result.ok(Collections.emptyList());

        // 3. barName
        Map<Long, String> barNameMap = barsService.listByIds(
                posts.stream().map(Post::getBarId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Bars::getId, Bars::getName));

        // 4. userName
        Map<Long, String> userNameMap = userService.listByIds(
                posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));

        // 5. 点赞状态
        Map<Long, Boolean> likedMap = new HashMap<>();
        if (userId != null) {
            List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            final Long uid = userId;

            List<Object> results = redis.executePipelined((RedisCallback<Object>) conn -> {
                for (Long pid : postIds) {
                    String key = "tieba:liked:post:" + pid;
                    conn.sIsMember(key.getBytes(), String.valueOf(uid).getBytes());
                }
                return null;
            });

            for (int i = 0; i < postIds.size(); i++) {
                Object r = results.get(i);
                boolean liked = (r instanceof Boolean && (Boolean) r)
                        || (r instanceof Long && (Long) r == 1L);
                likedMap.put(postIds.get(i), liked);
            }
        }

        // 6. 封装
        List<PostVO> list = posts.stream().map(p -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(p, vo);
            vo.setBarName(barNameMap.getOrDefault(p.getBarId(), "未知"));
            vo.setUserName(userNameMap.getOrDefault(p.getUserId(), "未知"));
            vo.setIsLiked(likedMap.getOrDefault(p.getId(), false));
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(list);
    }
}