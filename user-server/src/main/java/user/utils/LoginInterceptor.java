package user.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import user.domain.dto.UserDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static user.utils.RdeisConstants.LOGIN_USER_KEY;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    public LoginInterceptor(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // ⚠️ 注意：进入这个拦截器的请求，都是 MvcConfig 中配置的 needLoginPaths
        // 所以不需要再判断是否是公开路径，直接校验 token 即可

        // 获取 token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        token = token.substring(7);

        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = redis.opsForHash().entries(key);
        if (userMap == null || userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 封装用户
        UserDTO user = new UserDTO();
        Object idObj = userMap.get("id");
        if (idObj != null) user.setId(Long.valueOf(idObj.toString()));
        Object usernameObj = userMap.get("username");
        if (usernameObj != null) user.setUsername(usernameObj.toString());

        UserHolder.saveUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        // 请求结束后移除用户，避免内存泄漏
        UserHolder.removeUser();
    }
}