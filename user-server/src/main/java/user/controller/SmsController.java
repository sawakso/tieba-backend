package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.service.ISmsService;
import javax.annotation.Resource;

@RestController
@RequestMapping("/sms")
@Api(tags = "验证码接口")
public class SmsController {

    @Resource
    private ISmsService smsService;

    /**
     * 发送验证码接口
     * POST /api/sms/send
     * Body: {"email": "user@example.com"}
     */
    @ApiOperation("发送邮箱验证码")
    @PostMapping("/send")
    public Result sendCaptcha(@RequestParam String email,
                              @RequestParam String key) {
        return smsService.sendCaptchaEmail(email, key);
    }

    /**
     * 验证验证码接口
     * POST /api/sms/verify
     * Body: {"email": "user@example.com", "captcha": "123456"}
     */

}