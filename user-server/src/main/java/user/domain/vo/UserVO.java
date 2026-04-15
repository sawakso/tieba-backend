package user.domain.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    //用户名
    private String username;
    //昵称
    private String Nickname;
    //头像
    private String avatar;
    //个性签名
    private String signature;
    //性别
    private String gender;
    //等级
    private Integer level;
    //经验
    private Integer exp;
//    private Integer followers;
//    private Integer following;
}
