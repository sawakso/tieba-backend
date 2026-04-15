package user.domain.po;

import lombok.Data;

@Data
public class Captcha {
    private String email;  // 用户的邮箱
    private String code;   // 生成的验证码
}
