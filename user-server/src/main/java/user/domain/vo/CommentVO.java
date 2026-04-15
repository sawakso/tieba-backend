package user.domain.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;       // 显示用
    private String content;
    private Integer floor;
    private Integer likeCount;
    private Integer status;
    private Long replyToUserId;
    private Integer replyToFloor;
    private Date createTime;
    private Date updateTime;

    private List<CommentVO> children = new ArrayList<>();
    // ===== 新增字段 =====
    private String postTitle;           // 所属帖子标题
    private String replyToUserName;     // 被回复的用户名
    private String replyToContent;      // 被回复的评论内容（截断显示）
}
