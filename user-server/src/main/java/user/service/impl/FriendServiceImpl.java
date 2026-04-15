// user-server/src/main/java/user/service/impl/FriendServiceImpl.java
package user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.domain.po.FriendRequest;
import user.domain.po.User;
import user.domain.po.UserFriend;
import user.mapper.FriendRequestMapper;
import user.mapper.UserFriendMapper;
import user.mapper.UserMapper;
import user.service.IFriendService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends
        ServiceImpl<UserFriendMapper, UserFriend> implements IFriendService {

    private final UserFriendMapper userFriendMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFriendRequest(Long fromUserId, Long toUserId, String message) {
        // 不能添加自己为好友
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查是否已经是好友（用 MP 的 LambdaQueryWrapper）
        LambdaQueryWrapper<UserFriend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(UserFriend::getUserId, fromUserId)
                .eq(UserFriend::getFriendId, toUserId)
                .eq(UserFriend::getStatus, 1);
        Long friendCount = userFriendMapper.selectCount(friendWrapper);
        if (friendCount > 0) {
            throw new RuntimeException("你们已经是好友了");
        }

        // 检查是否已有待处理的申请
        LambdaQueryWrapper<FriendRequest> requestWrapper = new LambdaQueryWrapper<>();
        requestWrapper.eq(FriendRequest::getFromUserId, fromUserId)
                .eq(FriendRequest::getToUserId, toUserId)
                .eq(FriendRequest::getStatus, 0);
        Long requestCount = friendRequestMapper.selectCount(requestWrapper);
        if (requestCount > 0) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }

        // 检查对方是否已向你发送申请（如果有，直接同意）
        requestWrapper = new LambdaQueryWrapper<>();
        requestWrapper.eq(FriendRequest::getFromUserId, toUserId)
                .eq(FriendRequest::getToUserId, fromUserId)
                .eq(FriendRequest::getStatus, 0);
        FriendRequest reverseRequest = friendRequestMapper.selectOne(requestWrapper);
        if (reverseRequest != null) {
            // 对方已发申请，直接同意
            acceptFriendRequest(reverseRequest.getId(), fromUserId);
            return;
        }

        // 创建申请 - 使用 JDK 原生方式判断空字符串
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setMessage(message == null || message.trim().isEmpty() ? "请求添加您为好友" : message);
        request.setStatus(0);
        friendRequestMapper.insert(request);

        log.info("用户 {} 向用户 {} 发送了好友申请", fromUserId, toUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptFriendRequest(Long requestId, Long currentUserId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!request.getToUserId().equals(currentUserId)) {
            throw new RuntimeException("无权处理此申请");
        }
        if (request.getStatus() != 0) {
            throw new RuntimeException("申请已处理");
        }

        // 更新申请状态
        request.setStatus(1);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        // 双向添加好友（先检查是否已存在，避免重复插入）
        addFriendRelationIfNotExists(request.getFromUserId(), request.getToUserId());
        addFriendRelationIfNotExists(request.getToUserId(), request.getFromUserId());

        log.info("用户 {} 同意了用户 {} 的好友申请", currentUserId, request.getFromUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFriendRequest(Long requestId, Long currentUserId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!request.getToUserId().equals(currentUserId)) {
            throw new RuntimeException("无权处理此申请");
        }
        if (request.getStatus() != 0) {
            throw new RuntimeException("申请已处理");
        }

        request.setStatus(2);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        log.info("用户 {} 拒绝了用户 {} 的好友申请", currentUserId, request.getFromUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        // 删除双向关系（用 MP 的 LambdaQueryWrapper）
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId);
        userFriendMapper.delete(wrapper);

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, friendId)
                .eq(UserFriend::getFriendId, userId);
        userFriendMapper.delete(wrapper);

        log.info("用户 {} 删除了好友 {}", userId, friendId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void blockFriend(Long userId, Long friendId) {
        // 查找是否存在好友关系
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId);
        UserFriend friend = userFriendMapper.selectOne(wrapper);

        if (friend != null) {
            // 更新状态为拉黑
            friend.setStatus(3);
            userFriendMapper.updateById(friend);
        } else {
            // 创建拉黑关系
            friend = new UserFriend();
            friend.setUserId(userId);
            friend.setFriendId(friendId);
            friend.setStatus(3);
            userFriendMapper.insert(friend);
        }

        log.info("用户 {} 拉黑了用户 {}", userId, friendId);
    }

    @Override
    public List<User> getFriendList(Long userId) {
        // 用 MP 的 LambdaQueryWrapper 获取好友ID列表
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getStatus, 1)
                .select(UserFriend::getFriendId);

        List<UserFriend> friendRelations = userFriendMapper.selectList(wrapper);
        List<Long> friendIds = friendRelations.stream()
                .map(UserFriend::getFriendId)
                .collect(Collectors.toList());

        if (friendIds.isEmpty()) {
            return new ArrayList<>();  // JDK 8 兼容方式
        }

        // 查询用户信息
        return userMapper.selectBatchIds(friendIds);
    }

    @Override
    public List<FriendRequest> getPendingRequests(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId)
                .eq(FriendRequest::getStatus, 0)
                .orderByDesc(FriendRequest::getCreateTime);

        return friendRequestMapper.selectList(wrapper);
    }

    @Override
    public boolean isFriend(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            return true; // 自己是自己的好友
        }

        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, targetUserId)
                .eq(UserFriend::getStatus, 1);

        return userFriendMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Set<Long> getFriendIds(Long userId) {
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getStatus, 1)
                .select(UserFriend::getFriendId);

        List<UserFriend> friends = userFriendMapper.selectList(wrapper);
        return friends.stream()
                .map(UserFriend::getFriendId)
                .collect(Collectors.toSet());
    }

    /**
     * 添加好友关系（如果不存在）
     */
    private void addFriendRelationIfNotExists(Long userId, Long friendId) {
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId);

        UserFriend existFriend = userFriendMapper.selectOne(wrapper);
        if (existFriend == null) {
            UserFriend friend = new UserFriend();
            friend.setUserId(userId);
            friend.setFriendId(friendId);
            friend.setStatus(1);
            userFriendMapper.insert(friend);
        } else if (existFriend.getStatus() != 1) {
            // 如果存在但状态不是已确认，更新为已确认
            existFriend.setStatus(1);
            userFriendMapper.updateById(existFriend);
        }
    }
}