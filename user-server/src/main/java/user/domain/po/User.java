package user.domain.po;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 用户实体类
 * @author tieba
 * @date 2024-01-01
 */
@Data
@TableName("users")  // 对应数据库表名
public class User {

    /**
     * 用户ID，主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 密码，MD5加密存储
     */
    private String password;

    /**
     * 手机号，唯一
     */
    private String phone;

    /**
     * 邮箱，唯一
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 用户等级
     */
    private Integer level;

    /**
     * 经验值
     */
    private Integer exp;

    /**
     * 发帖总数
     */
    private Long postCount;

    /**
     * 回复总数
     */
    private Long replyCount;

    /**
     * 账号状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 注册时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间，插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}