// user-server/src/main/java/user/controller/InternalAuthController.java
package user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.domain.po.User;
import user.service.IUserService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static user.utils.RdeisConstants.LOGIN_USER_KEY;

@Api(tags = "Chat服务接口")
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    @Resource
    private StringRedisTemplate redis;

    @Resource
    private IUserService userService;

    /**
     * 供 chat-server 调用的 Token 验证接口
     */
    @ApiOperation(value = "验证Token", notes = "供内部服务调用，验证Token并返回用户信息")
    @GetMapping("/validate")
    public Result validateToken(@RequestHeader("Authorization") String tokenHeader) {

        System.out.println("=== InternalAuthController ===");
        System.out.println("收到的 tokenHeader: [" + tokenHeader + "]");
        System.out.println("tokenHeader 是否为 null: " + (tokenHeader == null));

        try {
            // 1. 解析 Token
            if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
                return Result.fail("无效的 Token");
            }
            String token = tokenHeader.substring(7);

            // 2. 从 Redis 获取用户信息
            String key = LOGIN_USER_KEY + token;
            Map<Object, Object> userMap = redis.opsForHash().entries(key);

            if (userMap == null || userMap.isEmpty()) {
                return Result.fail("Token 已失效或用户未登录");
            }

            // 3. 获取 userId
            Object idObj = userMap.get("id");
            if (idObj == null) {
                return Result.fail("用户信息不完整");
            }
            Long userId = Long.valueOf(idObj.toString());

            // 4. 获取完整用户信息
            User user = userService.getById(userId);
            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 5. 返回用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId().toString());
            userInfo.put("username", user.getUsername());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("email", user.getEmail());

            return Result.ok(userInfo);

        } catch (Exception e) {
            System.out.println("验证Token异常: " + e.getMessage());
            return Result.fail("验证失败: " + e.getMessage());
        }
    }
}