// user-server/src/main/java/user/controller/FriendController.java
package user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.domain.po.FriendRequest;
import user.domain.po.User;
import user.domain.vo.FriendVO;
import user.service.IFriendService;
import user.utils.UserHolder;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final IFriendService friendService;

    /**
     * 发送好友申请
     */
    @PostMapping("/request/send")
    public Result sendRequest(@RequestParam Long toUserId,
                              @RequestParam(required = false) String message) {
        Long currentUserId = UserHolder.getUser().getId();
        friendService.sendFriendRequest(currentUserId, toUserId, message);
        return Result.ok();
    }

    /**
     * 同意好友申请
     */
    @PostMapping("/request/accept/{requestId}")
    public Result acceptRequest(@PathVariable Long requestId) {
        Long currentUserId = UserHolder.getUser().getId();
        friendService.acceptFriendRequest(requestId, currentUserId);
        return Result.ok();
    }

    /**
     * 拒绝好友申请
     */
    @PostMapping("/request/reject/{requestId}")
    public Result rejectRequest(@PathVariable Long requestId) {
        Long currentUserId = UserHolder.getUser().getId();
        friendService.rejectFriendRequest(requestId, currentUserId);
        return Result.ok();
    }

    /**
     * 获取待处理的好友申请列表
     */
    @GetMapping("/request/pending")
    public Result getPendingRequests() {
        Long currentUserId = UserHolder.getUser().getId();
        List<FriendRequest> requests = friendService.getPendingRequests(currentUserId);
        return Result.ok(requests);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result getFriendList() {
        Long currentUserId = UserHolder.getUser().getId();
        List<User> friends = friendService.getFriendList(currentUserId);
        List<FriendVO> friendVOs = friends.stream().map(user -> {
            FriendVO vo = new FriendVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(friendVOs);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result deleteFriend(@PathVariable Long friendId) {
        Long currentUserId = UserHolder.getUser().getId();
        friendService.deleteFriend(currentUserId, friendId);
        return Result.ok();
    }

    /**
     * 拉黑好友
     */
    @PostMapping("/block/{friendId}")
    public Result blockFriend(@PathVariable Long friendId) {
        Long currentUserId = UserHolder.getUser().getId();
        friendService.blockFriend(currentUserId, friendId);
        return Result.ok();
    }
}