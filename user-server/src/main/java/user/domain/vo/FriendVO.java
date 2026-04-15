// user-server/src/main/java/user/domain/vo/FriendVO.java
package user.domain.vo;

import lombok.Data;

@Data
public class FriendVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}