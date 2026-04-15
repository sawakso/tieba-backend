package user.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

//帖子

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;

/**
 * 帖子实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("posts")
public class Post {

    /**
     * 帖子ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属贴吧ID，关联bars表
     */
    private Long barId;

    /**
     * 发帖用户ID，关联users表
     */
    private Long userId;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 帖子类型：0-普通，1-置顶，2-精华
     */
    private Integer type;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 回复次数
     */
    private Integer replyCount;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 帖子状态：0-删除，1-正常，2-审核中
     */
    private Integer status;

    /**
     * 最后回复时间
     */
    private Date lastReplyTime;

    /**
     * 最后回复用户ID，关联users表
     */
    private Long lastReplyUserId;

    /**
     * 发帖时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间，插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @TableField(exist = false)  // ⭐ 数据库没有这个字段
    private Boolean isLiked;
}