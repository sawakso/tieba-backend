// user-server/src/main/java/user/domain/dto/InternalResult.java
package user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalResult<T> {
    private Integer code;      // 200-成功，其他-失败
    private String message;
    private T data;

    public static <T> InternalResult<T> success(T data) {
        return new InternalResult<>(200, "success", data);
    }

    public static <T> InternalResult<T> fail(String message) {
        return new InternalResult<>(500, message, null);
    }

    public boolean isSuccess() {
        return code != null && code == 200;
    }
}