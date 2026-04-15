package user.domain.po;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 贴吧分类实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("bars_categories")
public class BarCategory {

    /**
     * 分类ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称，唯一
     */
    private String name;

    /**
     * 排序序号，数字越小越靠前
     */
    private Long sortOrder;

    /**
     * 状态：0-禁用，1-正常
     */
    private byte status;
}