package user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import user.domain.dto.Result;
import user.domain.po.Comment;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ICommentService extends IService<Comment> {

    Result addComment(Comment comment);

    Result getCommentsTree(Long postId);

    Result removeWithChildren(Long commentId);

    // 修改：ID类型从 Integer 改为 Long
    Comment getCommentById(Long id);

    // 修改：ID类型从 Integer 改为 Long
    List<Comment> getCommentsByIds(List<Long> ids);

    Result countByPostId(Long postId);

    Result queryByPostId(Long postId, Integer page, Integer size);  // 修改：分页参数改为 Integer

    Result queryMyComments(HttpServletRequest request, Integer page, Integer size);
}