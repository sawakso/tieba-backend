package user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.dto.Result;
import user.domain.po.UserBar;

public interface IUserBarService extends IService<UserBar> {
    Result followBar(Long userId, Long barId);

    Result unfollowBar(Long userId, Long barId);

    Result checkFollow(Long userId, Long barId);

    Result getUserFollowedBars(Long userId, Integer page, Integer size);

    Result getBarFollowers(Long barId, Integer page, Integer size);

    Result getUserFollowCount(Long userId);
}
