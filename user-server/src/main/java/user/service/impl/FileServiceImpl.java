package user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import user.domain.dto.UserDTO;
import user.domain.po.FilePO;
import user.mapper.FileMapper;
import user.service.IFileService;
import user.utils.UserHolder;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FilePO>
        implements IFileService {

    @Autowired
    private S3Client s3Client;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-url:}")
    private String publicUrl;

    @Value("${r2.account-id}")
    private String accountId;

    @Override
    public String upload(MultipartFile file, String bizType, Long bizId) {
        try {
            // 1. 生成文件 key
            String key = "upload/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            log.info("开始上传文件: bucket={}, key={}, size={}", bucket, key, file.getSize());

            // 2. 上传到 R2
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            log.info("R2 上传成功: {}", key);

            // 3. 获取当前用户
            UserDTO user = UserHolder.getUser();
            Long userId = user != null ? user.getId() : 0L;

            // 4. 保存文件记录到数据库
            FilePO f = new FilePO();
            f.setUserId(userId);
            f.setBizType(bizType);
            f.setBizId(bizId != null ? bizId : 0L);
            f.setFileKey(key);
            f.setFileName(file.getOriginalFilename());
            f.setFileType(file.getContentType());

            // 5. 构建访问 URL
            if (publicUrl != null && !publicUrl.isEmpty()) {
                f.setUrl(publicUrl + "/" + key);
            } else {
                // 使用 R2 默认公开域名
                f.setUrl("https://pub-" + accountId + ".r2.dev/" + key);
            }

            this.save(f);
            log.info("文件记录保存成功: id={}", f.getId());

            return key;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FilePO> listByBiz(String bizType, Long bizId) {
        return this.list(
                new QueryWrapper<FilePO>()
                        .eq("biz_type", bizType)
                        .eq("biz_id", bizId)
        );
    }
}