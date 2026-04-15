package chat.feign;

import chat.config.FeignConfig;
import chat.domain.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-server",
        url = "http://localhost:8880",  // ✅ 添加这一行
        contextId = "userFriendClient",
        configuration = FeignConfig.class
)
public interface UserFriendClient {

    /**
     * 检查好友关系
     */
    @GetMapping("/internal/friend/check")
    Result checkFriendship(@RequestParam("userId") Long userId,
                           @RequestParam("targetUserId") Long targetUserId);

    /**
     * 获取好友ID集合
     */
    @GetMapping("/internal/friend/ids/{userId}")
    Result getFriendIds(@PathVariable("userId") Long userId);
}