package ewm.comment;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.CommentUpdateDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/admin/comments")
public class CommentAdminController {
    private final CommentService commentService;

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@Valid @RequestBody CommentUpdateDto updateCommentDto,
                                    @PathVariable Long commentId) {
        return commentService.updateCommentByAdmin(updateCommentDto, commentId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
    }
}