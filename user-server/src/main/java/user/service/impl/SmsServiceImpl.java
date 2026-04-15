package user.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import user.domain.dto.Result;
import user.service.ISmsService;
import user.utils.RegexPatterns;
import user.utils.SmsCodeMessage;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static user.utils.RdeisConstants.LOGIN_CODE_KEY;
import static user.utils.RdeisConstants.SMS_CODE;
import static user.utils.SmsCodeMessage.*;

@Slf4j
@Service
public class SmsServiceImpl implements ISmsService {

    @Resource
    private JavaMailSender mailSender;  // Spring 提供的邮件发送功能

    @Resource
    private StringRedisTemplate redis;
    @Value("${spring.mail.username}")
    private String from;


    // 发送验证码到邮箱并存储到 Redis
    @Override
    public Result sendCaptchaEmail(String email,String key) {
        // 参数校验
        if (email == null || email.trim().isEmpty()) {
            return Result.fail("邮箱地址不能为空");
        }
        if (!email.matches(RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("邮箱格式错误");
        }

        // 生成验证码
        String captchaCode = generateCaptcha();
        log.info("发送验证码到邮箱：{}，验证码：{}", email, captchaCode);


        String codeMsg;
        switch (key) {
            case "login":
                codeMsg = LOGIN_MSG;
                break;
            case "register":
                codeMsg = REGISTER_MSG;
                break;
            case "updatePassword":
                codeMsg = UPDATE_PASSWORD_MSG;
                break;
            default:
                return Result.fail("发送验证码失败，请检查key"); // 或抛出异常
        }
        // 存储验证码到 Redis，设置过期时间为 5 分钟
        String redisKey = SMS_CODE + key +":" + email;
        redis.opsForValue().set(redisKey, captchaCode, 5, TimeUnit.MINUTES);
        // 发送邮件（内容包括验证码）
        String messageContent = "您的验证码是：" + captchaCode + "。" + codeMsg + "5分钟内有效。";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);  // 必须与配置中的 username 一致
            helper.setTo(email);
            helper.setSubject("身份验证码");
            helper.setText(messageContent, false);  // false 表示不是HTML格式

            mailSender.send(message);  // 发送邮件
            return Result.ok("验证码邮件发送成功：" + email + "验证码:" + captchaCode);

        } catch (MessagingException e) {
            log.error("发送验证码邮件失败：{}", email, e);
            // 发送失败时，删除 Redis 中已存储的验证码
            redis.delete(redisKey);
            return Result.fail("发送验证码邮件失败");
        }
    }


    // 生成6位数字验证码
    private String generateCaptcha() {
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            captcha.append(random.nextInt(10));  // 生成数字验证码
        }
        return captcha.toString();
    }

}