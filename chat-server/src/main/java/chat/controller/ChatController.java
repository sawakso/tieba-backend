package chat.controller;

import chat.domain.dto.Result;
import chat.domain.pojo.ChatMessage;
import chat.service.ChatMessageService;
import chat.websocket.ChatWebSocketHandler;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor  // 添加这个注解
public class ChatController {

    private final ChatMessageService chatMessageService;  // 注入 Service

    // 获取在线用户列表
    @GetMapping("/online-users")
    public Map<String, Object> getOnlineUsers() {
        Set<String> onlineUserIds = ChatWebSocketHandler.getOnlineUsers().keySet();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", onlineUserIds);
        result.put("total", onlineUserIds.size());
        return result;
    }

    // 检查指定用户是否在线
    @GetMapping("/user/{userId}/online")
    public Map<String, Object> checkUserOnline(@PathVariable String userId) {
        boolean online = ChatWebSocketHandler.getOnlineUsers().containsKey(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("userId", userId);
        result.put("online", online);
        return result;
    }

    // 🔥 新增：获取聊天记录
    @GetMapping("/history/{userId}/{targetId}")
    public Map<String, Object> getChatHistory(@PathVariable String userId,
                                              @PathVariable String targetId,
                                              @RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "20") long size) {
        try {
            Page<ChatMessage> page = chatMessageService.getConversationMessages(userId, targetId, current, size);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("success", true);

            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("current", page.getCurrent());
            data.put("size", page.getSize());

            result.put("data", data);
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("success", false);
            error.put("errorMsg", e.getMessage());
            return error;
        }
    }
}