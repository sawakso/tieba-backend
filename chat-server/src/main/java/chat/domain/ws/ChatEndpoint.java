package chat.domain.ws;


import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
@Component
public class ChatEndpoint {

    //用来存储每一个客户端对象用户对应的ChatEndpoint对象
    private static Map<String, ChatEndpoint> onlineUsers = new ConcurrentHashMap<String, ChatEndpoint>();
    //声明Session对象,用来与当前客户端对象进行通信
    private Session session;
    //声明一个HttpSession对象，用来获取当前用户
    private HttpSession httpSession;
    @OnOpen
    //连接建立时被调用
    public void onOpen(Session  session, EndpointConfig config) {
        System.out.println("用户连接");
        this.session = session;

    }
    @OnMessage
    //收到消息时被调用
    public void onMessage(String message,Session  session) {
        System.out.println("用户发送消息：" + message);
    }
    @OnClose
    //连接关闭时被调用
    public void onClose() {
        System.out.println("用户断开连接");
    }
    @OnError
    //发生错误时被调用
    public void onError(Throwable error) {
        System.out.println("用户错误");
    }

}
