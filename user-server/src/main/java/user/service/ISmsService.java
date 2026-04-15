package user.service;

import user.domain.dto.Result;

public interface ISmsService {

    /**
     * 发送验证码到邮箱
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    Result sendCaptchaEmail(String email,String key);

    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param inputCaptcha 用户输入的验证码
     * @return 是否验证成功
     */
//    boolean verifyCaptcha(String email, String inputCaptcha);
}