package user.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import user.domain.dto.UserDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static user.utils.RdeisConstants.LOGIN_USER_KEY;
import static user.utils.RdeisConstants.LOGIN_USER_TTL;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    public RefreshTokenInterceptor(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. 获取 Token
        String token = request.getHeader("Authorization");

        // 2. 校验 Token 格式
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        // 3. 去掉 Bearer 前缀
        token = token.substring(7);
        String key = LOGIN_USER_KEY + token;

        // 4. 从 Redis 获取用户信息
        Map<Object, Object> userMap = redis.opsForHash().entries(key);

        // 5. 校验用户是否存在
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 6. 封装用户信息
        UserDTO userDTO = new UserDTO();
        Object idObj = userMap.get("id");
        if (idObj != null) {
            userDTO.setId(Long.valueOf(idObj.toString()));
        }
        Object usernameObj = userMap.get("username");
        if (usernameObj != null) {
            userDTO.setUsername(usernameObj.toString());
        }

        // 7. 存入 ThreadLocal
        UserHolder.saveUser(userDTO);

        // 8. 刷新 Token 过期时间
        redis.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 9. 请求结束后清理 ThreadLocal，防止内存泄漏
        UserHolder.removeUser();
    }
}