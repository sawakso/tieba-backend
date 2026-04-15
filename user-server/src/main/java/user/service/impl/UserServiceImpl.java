package user.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import user.domain.dto.LoginForm;
import user.domain.dto.Result;
import user.domain.dto.UserDTO;
import user.domain.po.User;
import user.domain.vo.LoginVO;
import user.domain.vo.UserVO;
import user.mapper.UserMapper;
import user.service.IUserService;
import user.utils.RegexPatterns;
import user.utils.UserHolder;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static user.utils.RdeisConstants.*;



@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements IUserService {

    @Resource
    private StringRedisTemplate redis;


    @Override
    public Result login(String  username,String  password) {
        // 1. 查用户
        User user = lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        if (user == null || !user.getPassword().equals(password)) {
            return Result.fail("账号或密码错误");
        }

        // 2. 生成token
        String token = UUID.randomUUID().toString();

        // 3. 创建 LoginVO 并设置数据
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(String.valueOf(user.getId()));  // 将Long转为String
        loginVO.setUsername(user.getUsername());

        // 4. 转map存Redis（用于后续验证）
        Map<String, String> map = new HashMap<>();
        map.put("id", user.getId().toString());
        map.put("username", user.getUsername());

        // 5. 存Redis
        String key = LOGIN_USER_KEY + token;
        redis.opsForHash().putAll(key, map);
        redis.expire(key, 300, TimeUnit.MINUTES);

        // 6. 返回 LoginVO 对象
        return Result.ok(loginVO);
    }
    @Override
    public Result getUserInfo(Long id) {
        User user = getById(id);
        if (user == null) return Result.fail("用户不存在");

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.ok(vo);
    }

    @Override
    public Result register(LoginForm loginForm) {
        //1.校验邮箱
        String email = loginForm.getEmail();
        if (email == null || !email.matches(RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("邮箱格式错误");
        }
        //2.从redis获取验证码
        Object cachecode = redis.opsForValue().get(SMS_CODE + "register:" + email);
        System.out.println(cachecode);
        String code = loginForm.getCode();
        if(cachecode == null || !cachecode.toString().equals(code)){
            //不一致，报错
            return Result.fail("验证码错误");
        }
        // 1. 判重
        User exist = lambdaQuery()
                .eq(User::getUsername, loginForm.getUsername())
                .one();
        if (exist != null) {
            return Result.fail("用户名已存在");
        }

        // 2. 创建用户
        User user = new User();
        user.setUsername(loginForm.getUsername());
        user.setPassword(loginForm.getPassword());
        user.setEmail(loginForm.getEmail());// 建议加密

        save(user);

        return Result.ok("注册成功");
    }

    @Override
    public Result sendCode(String email) {
        //1.校验手机号
        if (email == null || !email.matches(RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("手机号格式错误");
        }
        //2. 生成6位验证码
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        //3.保存验证码到redis
        redis.opsForValue().set(
                LOGIN_CODE_KEY + email,code,
                5, TimeUnit.MINUTES);
        log.debug("发送短信验证码成功：{}",code);
        return Result.ok("发送验证码成功");
    }

    @Override
    public Result getCurrentUser() {
        UserDTO user = UserHolder.getUser();

        if (user == null) {
            return Result.fail("未登录");
        }

        User dbUser = getById(user.getId());

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(dbUser, vo);

        return Result.ok(vo);
    }

    @Override
    public Result updateUser(Long id) {
        UserVO vo = new UserVO();

        return Result.ok();
    }

    @Override
    public Result updatePassword(String newPassword, String oldPassword,Long id) {
        //校验用户是否存在
        User user = getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        //校验旧密码
        if (!user.getPassword().equals(oldPassword)) {
            return Result.fail("旧密码错误");
        }
        //正确，将新密码保存到数据库
        user.setPassword(newPassword);
        //返回成功
        return Result.ok();
    }

    @Override
    public Result updatePasswordByCode(String newPassword, String code, Long id) {
        //校验用户是否存在
        User user = getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        //从redis中获取验证码
        String cacheCode = redis.opsForValue().get(LOGIN_CODE_KEY + user.getEmail());
        //1.校验验证码
        if (cacheCode == null || !cacheCode.equals(code)) {
            //错误，返回失败
            return Result.fail("验证码错误");
        }
        //正确，将新密码保存到数据库
        user.setPassword(newPassword);
        //删除验证码
        //redis.delete(LOGIN_CODE_KEY + user.getEmail());
        //返回成功
        return Result.ok("修改密码成功，请记好您的新密码。");
    }

    @Override
    public Result loginByCode(String email, String code) {
        //校验邮箱
        if (email == null || !email.matches(RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("邮箱错误!");
        }
        //根据邮箱查询用户
        User user = lambdaQuery().one() ;
        if (user == null) {
            return Result.fail("用户不存在!");
        }
        //从redis中获取验证码
        String cacheCode = redis.opsForValue().get(LOGIN_CODE_KEY + email);
        //校验验证码
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        return Result.ok("登录成功!");
    }

}
