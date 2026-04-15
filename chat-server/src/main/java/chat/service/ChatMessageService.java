package chat.service;

import chat.domain.dto.ChatMessageDTO;
import chat.domain.pojo.ChatMessage;
import chat.domain.pojo.ChatConversation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 保存消息到数据库
     */
    void saveMessage(ChatMessageDTO msgDTO);

    /**
     * 获取离线消息
     */
    List<ChatMessage> getOfflineMessages(String userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(String msgId, String readerId);

    /**
     * 批量标记消息为已送达
     */
    void batchMarkAsDelivered(List<String> msgIds);

    /**
     * 分页查询聊天记录
     */
    Page<ChatMessage> getConversationMessages(String userId1, String userId2,
                                              long current, long size);
}

