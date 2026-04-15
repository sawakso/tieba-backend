package user.domain.dto;

import lombok.Data;

@Data
public class LoginForm {
    private String username;
    private String password;
    private String email;
    private String code;
    private String key;
}
