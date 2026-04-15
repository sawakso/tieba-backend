package user.service;
import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.dto.LoginForm;
import user.domain.dto.Result;
import user.domain.po.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {
    Result login(String username,String password);

    Result getUserInfo(Long id);

    Result register(LoginForm loginForm);

    Result sendCode(String phone);

    Result getCurrentUser();

    Result updateUser(Long id);

    Result updatePassword(String newPassword, String oldPassword,Long id);

    Result updatePasswordByCode(String newPassword, String code,Long id);

    Result loginByCode(String email, String code);
}
