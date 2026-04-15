package chat.mapper;

import chat.domain.pojo.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询两个用户之间的聊天记录（分页）
     */
    @Select("SELECT * FROM chat_message WHERE " +
            "((from_user_id = #{userId1} AND to_user_id = #{userId2}) OR " +
            "(from_user_id = #{userId2} AND to_user_id = #{userId1})) " +
            "ORDER BY send_time DESC LIMIT #{offset}, #{limit}")
    List<ChatMessage> findConversationMessages(@Param("userId1") String userId1,
                                               @Param("userId2") String userId2,
                                               @Param("offset") long offset,
                                               @Param("limit") long limit);

    /**
     * 获取用户的离线消息
     */
    @Select("SELECT * FROM chat_message WHERE to_user_id = #{userId} " +
            "AND status IN (1, 2) ORDER BY send_time ASC")
    List<ChatMessage> findOfflineMessages(@Param("userId") String userId);

    /**
     * 批量更新消息状态
     */
    @Update("UPDATE chat_message SET status = #{status} WHERE msg_id = #{msgId}")
    int updateMessageStatus(@Param("msgId") String msgId, @Param("status") Integer status);

    /**
     * 标记消息为已读
     */
    @Update("UPDATE chat_message SET status = 3, read_time = #{readTime} " +
            "WHERE msg_id = #{msgId}")
    int markAsRead(@Param("msgId") String msgId, @Param("readTime") LocalDateTime readTime);
}