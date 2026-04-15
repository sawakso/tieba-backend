// user-server/src/main/java/user/controller/InternalFriendController.java
package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.service.IFriendService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Api(tags = "Chat服务内部接口-好友")
@RestController
@RequestMapping("/internal/friend")
@RequiredArgsConstructor
public class InternalFriendController {

    private final IFriendService friendService;

    /**
     * 供 chat-server 调用的好友关系检查接口
     */
    @ApiOperation(value = "检查好友关系", notes = "供内部服务调用，检查两个用户是否为好友")
    @GetMapping("/check")
    public Result checkFriendship(@RequestParam("userId") Long userId,
                                  @RequestParam("targetUserId") Long targetUserId) {

        System.out.println("=== InternalFriendController.checkFriendship ===");
        System.out.println("userId: " + userId + ", targetUserId: " + targetUserId);

        try {
            boolean isFriend = friendService.isFriend(userId, targetUserId);

            Map<String, Object> data = new HashMap<>();
            data.put("isFriend", isFriend);
            data.put("userId", userId);
            data.put("targetUserId", targetUserId);

            return Result.ok(data);
        } catch (Exception e) {
            System.out.println("检查好友关系异常: " + e.getMessage());
            return Result.fail("检查失败: " + e.getMessage());
        }
    }

    /**
     * 供 chat-server 调用的获取好友ID列表接口
     */
    @ApiOperation(value = "获取好友ID列表", notes = "供内部服务调用，获取用户的所有好友ID")
    @GetMapping("/ids/{userId}")
    public Result getFriendIds(@PathVariable Long userId) {

        System.out.println("=== InternalFriendController.getFriendIds ===");
        System.out.println("userId: " + userId);

        try {
            Set<Long> friendIds = friendService.getFriendIds(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("friendIds", friendIds);
            data.put("userId", userId);
            data.put("count", friendIds.size());

            return Result.ok(data);
        } catch (Exception e) {
            System.out.println("获取好友列表异常: " + e.getMessage());
            return Result.fail("获取失败: " + e.getMessage());
        }
    }
}