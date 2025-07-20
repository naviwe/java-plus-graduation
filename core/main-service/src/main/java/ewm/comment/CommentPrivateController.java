package ewm.comment;

import ewm.comment.dto.CommentCreateDto;
import ewm.comment.dto.CommentDto;
import ewm.comment.dto.CommentUpdateDto;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentPrivateController {
    final String userIdHeader = "X-Sharer-User-Id";
    final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@Valid @RequestBody CommentCreateDto comment,
                                    @RequestHeader(value = userIdHeader) Long userId,
                                    @PathVariable Long eventId) {
        return commentService.createComment(comment, userId, eventId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(
            @RequestHeader(value = userIdHeader) Long userId,
            @PathVariable Long eventId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateDto updateCommentDto) {
        return commentService.updateCommentByUser(commentId, userId,eventId, updateCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @RequestHeader(value = userIdHeader) Long userId,
            @PathVariable Long eventId,
            @PathVariable Long commentId) {
        commentService.deleteCommentByUser(commentId, userId,eventId);
    }
}
