package chat.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;   // 会话ID（两个用户ID排序后拼接）
    private String userId;           // 当前用户ID
    private String targetId;         // 对方用户ID
    private String lastMsgId;        // 最后一条消息ID
    private String lastMsgContent;   // 最后一条消息内容
    private LocalDateTime lastMsgTime; // 最后一条消息时间
    private Integer unreadCount;     // 未读消息数
    private Integer isTop;           // 是否置顶：0-否 1-是
    private Integer isMuted;         // 是否免打扰：0-否 1-是
    private Integer isDeleted;       // 是否删除：0-否 1-是（软删除）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}