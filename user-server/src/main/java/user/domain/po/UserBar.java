package user.domain.po;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 用户关注贴吧关联实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("user_bars")
public class UserBar {

    /**
     * 关注记录ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联users表
     */
    private Long userId;

    /**
     * 贴吧ID，关联bars表
     */
    private Long barId;

    /**
     * 关注时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date followTime;
}