package chat.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String msgId;           // 消息唯一ID（雪花算法生成）
    private String fromUserId;
    private String toUserId;
    private Integer contentType;     // 1-文本 2-图片 3-语音 4-视频 5-文件
    private String content;
    private Integer status;          // 1-发送中 2-已送达 3-已读 4-发送失败

    private LocalDateTime sendTime;
    private LocalDateTime deliverTime;
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}