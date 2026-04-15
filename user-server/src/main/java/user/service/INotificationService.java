package user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.dto.Result;
import user.domain.po.Notification;

public interface INotificationService extends IService<Notification> {
    Result getNotifications(Integer userId, Integer page, Integer size);

    Result getUnreadCount(Integer userId);

    Result markAsRead(Integer notificationId);

    Result markAllAsRead(Integer userId);

    Result deleteNotification(Integer notificationId);

    Result batchDeleteNotifications(String notificationIds);
}
