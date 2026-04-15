package chat.mapper;

import chat.domain.pojo.ChatConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /**
     * 获取用户的会话列表（按置顶和时间排序）
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} " +
            "ORDER BY is_top DESC, last_msg_time DESC")
    List<ChatConversation> findUserConversations(@Param("userId") String userId);

    /**
     * 增加未读消息数
     */
    @Update("UPDATE chat_conversation SET unread_count = unread_count + #{delta} " +
            "WHERE user_id = #{userId} AND target_id = #{targetId}")
    int incrementUnreadCount(@Param("userId") String userId,
                             @Param("targetId") String targetId,
                             @Param("delta") int delta);

    /**
     * 清空未读消息数
     */
    @Update("UPDATE chat_conversation SET unread_count = 0 " +
            "WHERE user_id = #{userId} AND target_id = #{targetId}")
    int clearUnreadCount(@Param("userId") String userId,
                         @Param("targetId") String targetId);

    /**
     * 获取两个用户之间的会话
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} " +
            "AND target_id = #{targetId}")
    ChatConversation findConversation(@Param("userId") String userId,
                                      @Param("targetId") String targetId);
}