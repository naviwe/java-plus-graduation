package ewm.utils;

import ewm.comment.Comment;
import ewm.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ewm.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCommentService {
    private final CommentRepository commentRepository;

    public Comment checkComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d was not found",
                        commentId)));
    }
}