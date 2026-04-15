package chat.websocket;

import chat.domain.dto.Result;
import chat.feign.UserAuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private UserAuthClient userAuthClient;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        System.out.println("=== WebSocket 握手 ===");
        System.out.println("URL Query: " + query);

        String token = null;
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }

        System.out.println("提取的 token: " + token);

        if (token == null || token.isEmpty()) {
            System.err.println("Token 为空");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String authHeader = "Bearer " + token;
            System.out.println("传给 user-server 的 Header: " + authHeader);

            // 现在返回的是 Result 对象
            Result result = userAuthClient.validateToken(authHeader);
            System.out.println("user-server 返回: " + result);

            // 检查是否成功
            if (!result.getSuccess()) {
                System.err.println("Token 验证失败: " + result.getErrorMsg());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 从 data 中获取用户信息
            Map<String, Object> userInfo = (Map<String, Object>) result.getData();

            Object userIdObj = userInfo.get("userId");
            Object usernameObj = userInfo.get("username");
            Object avatarObj = userInfo.get("avatar");

            if (userIdObj == null) {
                System.err.println("userId 为空");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 确保存入的值不是 null
            attributes.put("userId", String.valueOf(userIdObj));
            attributes.put("username", usernameObj != null ? usernameObj.toString() : "");
            attributes.put("avatar", avatarObj != null ? avatarObj.toString() : "");

            System.out.println("握手成功！userId: " + userIdObj);

            return true;

        } catch (Exception e) {
            System.err.println("验证失败: " + e.getMessage());
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后处理
        if (exception != null) {
            System.err.println("WebSocket 握手异常: " + exception.getMessage());
        }
    }
}