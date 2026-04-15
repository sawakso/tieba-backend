package user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;              // ✅ Long - 主键

    private Long postId;          // ✅ Long - 帖子ID

    private Long userId;          // ✅ Long - 用户ID

    private String content;

    private Integer floor;        // ✅ Integer - 楼层（不是Long）

    private Integer likeCount;    // ✅ Integer - 点赞数（不是Long）

    private Integer status;       // ✅ Integer - 状态（不是byte）

    private Long replyToUserId;   // ✅ Long - 目标用户ID

    private Integer replyToFloor; // ✅ Integer - 目标楼层（不是Long）

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    private List<Comment> children;
}