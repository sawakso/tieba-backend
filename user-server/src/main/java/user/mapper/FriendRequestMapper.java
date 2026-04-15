// user-server/src/main/java/user/mapper/FriendRequestMapper.java
package user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import user.domain.po.FriendRequest;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
    // 所有方法用 MP 的 LambdaQueryWrapper 实现
}