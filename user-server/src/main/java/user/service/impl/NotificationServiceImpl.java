package user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import user.domain.dto.Result;
import user.domain.po.Notification;
import user.mapper.NotificationMapper;
import user.service.INotificationService;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements INotificationService {

    @Override
    public Result getNotifications(Integer userId, Integer page, Integer size) {
        return null;
    }

    @Override
    public Result getUnreadCount(Integer userId) {
        return null;
    }

    @Override
    public Result markAsRead(Integer notificationId) {
        return null;
    }

    @Override
    public Result markAllAsRead(Integer userId) {
        return null;
    }

    @Override
    public Result deleteNotification(Integer notificationId) {
        return null;
    }

    @Override
    public Result batchDeleteNotifications(String notificationIds) {
        return null;
    }
}