package user.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class CommentDetailDTO {
    private Integer id;
    private String content;
    private Integer postId;
    private String postTitle;
    private Integer userId;
    private String userNickname;
    private Date createTime;
}