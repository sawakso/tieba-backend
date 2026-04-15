package user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 通知消息实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("notifications")
public class Notification {

    /**
     * 通知ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID，关联users表
     */
    private Long userId;

    /**
     * 触发通知的用户ID，关联users表
     */
    private Long fromUserId;

    /**
     * 通知类型：reply-回复，like-点赞，follow-关注，system-系统
     */
    private Integer type;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 目标类型：1-帖子，2-回复
     */
    private Integer targetType;

    /**
     * 目标ID（帖子ID或回复ID）
     */
    private Long targetId;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 创建时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}