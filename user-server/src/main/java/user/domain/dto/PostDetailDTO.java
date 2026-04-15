package user.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PostDetailDTO {
    private Integer id;
    private String title;
    private String content;
    private Integer userId;
    private String userNickname;
    private Integer likeCount;
    private Integer commentCount;
    private Date createTime;
    private Date updateTime;
}