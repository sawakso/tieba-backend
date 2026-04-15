package user.config;

public class PathConstants {

    /**
     * 完全公开的路径（不需要任何拦截，连token都不需要解析）
     */
    public static final String[] PUBLIC_PATHS = {
            "/doc.html",
            "/favicon.ico",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/user/login",
            "/user/register",
            "/sms/**",
            "/internal/auth/validate",
            "/bars/page",
            "/bars/detail/**",
            "/bars/list",
            "/post/random",
            "/post/detail/**",
            "/post/list",
            "/post/queryByBarId",
            "/comment/list/**",
            "/file/**"

    };

    /**
     * 需要登录的路径（需要token校验）
     */
    public static final String[] NEED_LOGIN_PATHS = {
            "/user/info",
            "/user/update",
            "/user/current",
            "/bars/create",
            "/bars/delete/**",
            "/bars/update/**",
            "/post/create",
            "/post/update/**",
            "/post/delete/**",
            "/post/comment/add",
            "/post/queryMyPosts",
            "/post/queryMyComments",
            "/likes/**",
            "/notification/**",
            "/friend/**",
            "/chat/**",
            "/api/chat/**",
    };

    public static final boolean DEFAULT_NEED_LOGIN = true;
}