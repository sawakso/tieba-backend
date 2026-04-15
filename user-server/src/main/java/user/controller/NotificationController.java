package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.service.INotificationService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/notifications")
@Api(tags = "通知接口")
public class NotificationController {

    @Resource
    private INotificationService notificationService;

    @ApiOperation("获取用户的通知列表")
    @GetMapping("/user/{userId}")
    public Result getNotifications(@PathVariable Integer userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return notificationService.getNotifications(userId, page, size);
    }

    @ApiOperation("获取未读通知数量")
    @GetMapping("/user/{userId}/unread/count")
    public Result getUnreadCount(@PathVariable Integer userId) {
        return notificationService.getUnreadCount(userId);
    }

    @ApiOperation("标记通知为已读")
    @PutMapping("/{notificationId}/read")
    public Result markAsRead(@PathVariable Integer notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @ApiOperation("标记所有通知为已读")
    @PutMapping("/user/{userId}/read/all")
    public Result markAllAsRead(@PathVariable Integer userId) {
        return notificationService.markAllAsRead(userId);
    }

    @ApiOperation("删除通知")
    @DeleteMapping("/{notificationId}")
    public Result deleteNotification(@PathVariable Integer notificationId) {
        return notificationService.deleteNotification(notificationId);
    }

    @ApiOperation("批量删除通知")
    @DeleteMapping("/batch")
    public Result batchDeleteNotifications(@RequestParam String notificationIds) {
        return notificationService.batchDeleteNotifications(notificationIds);
    }
}