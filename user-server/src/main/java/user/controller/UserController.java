package user.controller;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.LoginForm;
import user.domain.dto.Result;
import user.domain.po.User;
import user.service.ICommentService;
import user.service.ILikeService;
import user.service.IPostService;
import user.service.IUserService;

import javax.annotation.Resource;

//登录相关
@RestController
@RequestMapping("/user")
@Api(tags = "用户接口")
public class UserController {

    @Resource
    private IUserService userService;
    @Resource
    private ICommentService commentService;
    @Resource
    private ILikeService likeService;
    @Resource
    private IPostService postService;
    @Autowired
    private StringRedisTemplate redis;

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody LoginForm loginForm) {
        return userService.register(loginForm);
    }
    @ApiOperation("用户密码登录")
    @PostMapping("/login")
    public Result login(@RequestParam String username,
                        @RequestParam String password) {
        return userService.login(username, password);
    }
    @ApiOperation("用户验证码登录")
    @PostMapping("/loginByCode")
    public Result loginByCode(@RequestParam String email,
                              @RequestParam String code) {
        return userService.loginByCode(email, code);
    }

    @ApiOperation("发送验证码")
    @PostMapping("/sendCode")
    public Result sendCode(@RequestParam String phone) {
        return userService.sendCode(phone);
    }

    @ApiOperation("用户登出")
    @PostMapping("/logout")
    public String logout() {
        return "已退出";
    }

    @ApiOperation("查询用户信息")
    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id) {
        return userService.getUserInfo(id);
    }
    @ApiOperation("通过token查询用户信息")
    @GetMapping("/current")
    public Result getCurrentUser() {
        return userService.getCurrentUser();
    }

    @ApiOperation("修改用户信息")
    @PostMapping("/update")
    public Result updateUser(@RequestParam Long id) {
        return userService.updateUser(id);
    }
    @ApiOperation("修改密码")
    @PostMapping("/updatePassword")
    public Result updatePassword(@RequestParam String newPassword,
                                 @RequestParam String oldPassword,
                                 @RequestParam Long id) {
        return userService.updatePassword(newPassword, oldPassword, id);
    }
    @ApiOperation("找回密码")
    @PostMapping("/retrieve")
    public Result updatePasswordByCode(@RequestParam String newPassword,
                                       @RequestParam String code,
                                       @RequestParam Long id) {
        return userService.updatePasswordByCode(newPassword, code,id);
    }



}