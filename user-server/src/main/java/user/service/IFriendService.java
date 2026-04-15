// user-server/src/main/java/user/service/IFriendService.java
package user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.po.FriendRequest;
import user.domain.po.User;
import user.domain.po.UserFriend;

import java.util.List;
import java.util.Set;

public interface IFriendService extends IService<UserFriend> {

    /**
     * 发送好友申请
     */
    void sendFriendRequest(Long fromUserId, Long toUserId, String message);

    /**
     * 同意好友申请
     */
    void acceptFriendRequest(Long requestId, Long currentUserId);

    /**
     * 拒绝好友申请
     */
    void rejectFriendRequest(Long requestId, Long currentUserId);

    /**
     * 删除好友
     */
    void deleteFriend(Long userId, Long friendId);

    /**
     * 拉黑好友
     */
    void blockFriend(Long userId, Long friendId);

    /**
     * 获取好友列表
     */
    List<User> getFriendList(Long userId);

    /**
     * 获取待处理的好友申请
     */
    List<FriendRequest> getPendingRequests(Long userId);

    /**
     * 检查是否为好友
     */
    boolean isFriend(Long userId, Long targetUserId);

    /**
     * 获取好友ID集合
     */
    Set<Long> getFriendIds(Long userId);
}