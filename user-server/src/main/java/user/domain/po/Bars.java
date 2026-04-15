package user.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;

/**
 * 贴吧实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("bars")
public class Bars {

    /**
     * 贴吧ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 贴吧名称，唯一
     */
    private String name;

    /**
     * 贴吧描述
     */
    private String description;

    /**
     * 贴吧Logo URL
     */
    private String logo;

    /**
     * 贴吧背景图URL
     */
    private String background;

    /**
     * 所属分类ID，关联bars_categories表
     */
    private Long categoryId;

    /**
     * 创建者用户ID，关联users表
     */
    private Long creatorId;

    /**
     * 成员数量
     */
    private Long memberCount;

    /**
     * 帖子数量
     */
    private Long postCount;

    /**
     * 贴吧状态：0-关闭，1-正常
     */
    private byte status;

    /**
     * 创建时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间，插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
