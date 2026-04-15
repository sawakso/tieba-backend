package user.domain.po;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 点赞记录实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("likes")
public class Like {

    /**
     * 点赞记录ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 点赞用户ID，关联users表
     */
    private Long userId;

    /**
     * 点赞目标类型：1-帖子，2-回复
     */
    private Integer targetType;

    /**
     * 点赞目标ID（帖子ID或回复ID）
     */
    private Long targetId;

    /**
     * 点赞时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}