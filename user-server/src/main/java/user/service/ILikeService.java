package user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.dto.Result;
import user.domain.po.Like;

/**
 * 点赞服务接口
 * @author tieba
 * @date 2024-01-01
 */
public interface ILikeService extends IService<Like> {

    /**
     * 点赞
     * @param targetType 目标类型：1-帖子，2-评论
     * @param targetId 目标ID
     * @return Result
     */
    Result like(Integer targetType, Long targetId);

    /**
     * 检查用户是否点赞
     * @param userId 用户ID
     * @param targetType 目标类型：1-帖子，2-评论
     * @param targetId 目标ID
     * @return Result，data中包含isLiked字段
     */
    Result checkLike(Long userId, Integer targetType, Long targetId);

    /**
     * 获取用户点赞的目标列表
     * @param userId 用户ID
     * @param targetType 目标类型：1-帖子，2-评论
     * @param page 页码
     * @param size 每页大小
     * @return Result，data为分页数据
     */
    Result getUserLikedTargets(Long userId, Integer targetType, Integer page, Integer size);
}
