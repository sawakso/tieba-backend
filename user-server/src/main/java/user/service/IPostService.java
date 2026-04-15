package user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.dto.Result;
import user.domain.po.Post;
import user.domain.vo.PostVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IPostService extends IService<Post> {
    Result queryMyPosts(Long userId, HttpServletRequest request);

    List<Post> listHotPosts(Integer limit);

    List<Post> getPostsByIds(List<Long> ids);

    Post getPostById(Long id);

    Result updatePostStatistics(Long postId);

    Result updateAllPostsStatistics();

    Result getPostList();

    Result random(Integer size, HttpServletRequest request);
}