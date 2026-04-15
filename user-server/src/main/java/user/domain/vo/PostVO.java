package user.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PostVO {
    private Long id;
    private Long barId;
    private String barName;  // 新增吧名
    private Long userId;
    private String userName;
    private String title;
    private String content;
    private Integer type;
    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer status;
    private Date lastReplyTime;
    private Long lastReplyUserId;
    private Date createTime;
    private Date updateTime;

    private Boolean isLiked;  // 当前用户是否点赞



}
