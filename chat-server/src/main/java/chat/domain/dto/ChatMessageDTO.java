package chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String MsgId;      // 消息ID
    private String fromUserId;     // 发送者ID
    private String fromUsername;   // 发送者昵称
    private String fromAvatar;     // 发送者头像
    private String toUserId;       // 接收者ID
    private String content;        // 消息内容
    private Integer contentType;     // 1-文本 2-图片 3-语音 4-视频 5-文件
    private Integer type;          // 1:文字 2:图片 3:文件
    private Long timestamp;        // 时间戳
    private Integer status;        // 0:发送中 1:已送达 2:已读
}