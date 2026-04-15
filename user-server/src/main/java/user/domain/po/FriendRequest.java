// user-server/src/main/java/user/domain/po/FriendRequest.java
package user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_friend_request")
public class FriendRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String message;

    /**
     * 状态：0-待处理，1-已同意，2-已拒绝
     */
    private Integer status;

    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}