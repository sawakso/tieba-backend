package chat.feign;

import chat.config.FeignConfig;
import chat.domain.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "user-server",
        url = "http://localhost:8880",  // ✅ 关键修改：添加实际地址
        contextId = "userAuthClient",
        configuration = FeignConfig.class
)
public interface UserAuthClient {

    /**
     * 验证 Token 并返回用户信息
     */
    @GetMapping("/internal/auth/validate")
    Result validateToken(@RequestHeader("Authorization") String token);
}