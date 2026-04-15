package user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import user.domain.dto.Result;
import user.domain.po.UserBar;
import user.mapper.UserBarMapper;
import user.service.IUserBarService;

@Slf4j
@Service
public class UserBarServiceImpl extends ServiceImpl<UserBarMapper, UserBar>
        implements IUserBarService {
    @Override
    public Result followBar(Long userId, Long barId) {
        UserBar ub = new UserBar();
        ub.setUserId(userId);
        ub.setBarId(barId);
        boolean saved = save(ub);
        return saved ? Result.ok() : Result.fail("关注失败");
    }

    @Override
    public Result unfollowBar(Long userId, Long barId) {
        LambdaQueryWrapper<UserBar> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBar::getUserId, userId)
                .eq(UserBar::getBarId, barId);
        boolean removed = remove(qw);
        return removed ? Result.ok() : Result.fail("取消失败");
    }

    @Override
    public Result checkFollow(Long userId, Long barId) {
        LambdaQueryWrapper<UserBar> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBar::getUserId, userId)
                .eq(UserBar::getBarId, barId);
        boolean exists = count(qw) > 0;
        return Result.ok(exists);
    }

    @Override
    public Result getUserFollowedBars(Long userId, Integer page, Integer size) {
        Page<UserBar> p = new Page<>(page, size);
        LambdaQueryWrapper<UserBar> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBar::getUserId, userId);
        page(p, qw);
        return Result.ok(p);
    }

    @Override
    public Result getBarFollowers(Long barId, Integer page, Integer size) {
        Page<UserBar> p = new Page<>(page, size);
        LambdaQueryWrapper<UserBar> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBar::getBarId, barId);
        page(p, qw);
        return Result.ok(p);
    }

    @Override
    public Result getUserFollowCount(Long userId) {
        LambdaQueryWrapper<UserBar> qw = new LambdaQueryWrapper<>();
        qw.eq(UserBar::getUserId, userId);
        return Result.ok(count(qw));
    }
}
