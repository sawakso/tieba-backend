package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.service.IUserBarService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/userBar")
@Api(tags = "用户贴吧关系接口")
public class UserBarController {

    @Resource
    private IUserBarService userBarService;

    @ApiOperation("关注贴吧")
    @PostMapping("/follow")
    public Result followBar(@RequestParam Long userId,
                            @RequestParam Long barId) {
        return userBarService.followBar(userId, barId);
    }

    @ApiOperation("取消关注贴吧")
    @DeleteMapping("/unfollow")
    public Result unfollowBar(@RequestParam Long userId,
                              @RequestParam Long barId) {
        return userBarService.unfollowBar(userId, barId);
    }

    @ApiOperation("检查是否关注贴吧")
    @GetMapping("/check")
    public Result checkFollow(@RequestParam Long userId,
                              @RequestParam Long barId) {
        return userBarService.checkFollow(userId, barId);
    }

    @ApiOperation("获取用户关注的贴吧列表")
    @GetMapping("/user/{userId}/bars")
    public Result getUserFollowedBars(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        return userBarService.getUserFollowedBars(userId, page, size);
    }

    @ApiOperation("获取贴吧的关注用户列表")
    @GetMapping("/bar/{barId}/users")
    public Result getBarFollowers(@PathVariable Long barId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        return userBarService.getBarFollowers(barId, page, size);
    }

    @ApiOperation("获取用户的关注数量")
    @GetMapping("/user/{userId}/count")
    public Result getUserFollowCount(@PathVariable Long userId) {
        return userBarService.getUserFollowCount(userId);
    }
}