package ewm.comment;

import ewm.comment.dto.CommentDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("events/{eventId}/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentPublicController {
    CommentService commentService;

    @GetMapping
    public List<CommentDto> findCommentsByEventId(@RequestParam(defaultValue = "0") Integer from,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @PathVariable Long eventId) {
        return commentService.findCommentsByEventId(eventId, from, size);
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable Long commentId,
                                 @PathVariable Long eventId) {
        return commentService.getComment(commentId,eventId);
    }
}