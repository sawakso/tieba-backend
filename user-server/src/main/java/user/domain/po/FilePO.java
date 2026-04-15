package user.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file")
public class FilePO {

    private Long id;

    private Long userId;

    private String bizType;   // avatar/post/comment/cover/background

    private Long bizId;

    private String fileKey;   // R2路径

    private String fileName;

    private String fileType;  // image/video

//    private Long fileSize;

    private String url;

    private LocalDateTime createTime;
}