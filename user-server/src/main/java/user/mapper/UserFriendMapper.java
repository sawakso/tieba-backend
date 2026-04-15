// user-server/src/main/java/user/mapper/UserFriendMapper.java
package user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import user.domain.po.UserFriend;

@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {
    // 所有方法用 MP 的 LambdaQueryWrapper 实现，不需要写 SQL
}