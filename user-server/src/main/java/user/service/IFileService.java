package user.service;

import org.springframework.web.multipart.MultipartFile;
import user.domain.po.FilePO;

import java.util.List;

public interface IFileService {

    String upload(MultipartFile file, String bizType, Long bizId);

    List<FilePO> listByBiz(String bizType, Long bizId);
}