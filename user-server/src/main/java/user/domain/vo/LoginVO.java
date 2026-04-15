package user.domain.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String userId;
    private String username;
}
