package chat.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 配置类
 */
@Configuration
public class FeignConfig {

    /**
     * 请求拦截器：传递认证信息
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                // 获取原始请求的 Authorization 头
                String authorization = attributes.getRequest().getHeader("Authorization");
                if (authorization != null) {
                    // 传递给 user-server
                    requestTemplate.header("Authorization", authorization);
                }

                // 也可以传递其他需要的头信息
                String userId = attributes.getRequest().getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }
            }
        };
    }
}