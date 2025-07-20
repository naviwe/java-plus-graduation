package ewm.comment;

import ewm.comment.dto.CommentCreateDto;
import ewm.comment.dto.CommentDto;
import ewm.comment.dto.CommentUpdateDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.event.Event;
import ewm.exception.ForbiddenException;
import ewm.exception.ValidationException;
import ewm.utils.CheckCommentService;
import ewm.utils.CheckEventService;
import ewm.utils.CheckUserService;

import java.time.LocalDateTime;
import java.util.List;

import static ewm.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImpl implements CommentService {
    CheckUserService checkUserService;
    CommentRepository commentRepository;
    CheckEventService checkEventService;
    CommentMapper commentMapper;
    CheckCommentService checkCommentService;

    @Override
    public List<CommentDto> findCommentsByEventId(Long eventId, Integer from, Integer size) {
        Pageable pageRequest = PageRequest.of(from / size, size);
        Page<Comment> commentPage = commentRepository.findByEventId(eventId, pageRequest);
        return logAndReturn(commentPage.getContent()
                        .stream()
                        .map(commentMapper::toDto)
                        .toList(),
                comments -> log.info("Found {} comments for event with id= {}",
                        comments.size(), eventId)
        );
    }

    @Override
    public CommentDto getComment(Long commentId,Long eventId) {
        Comment comment = checkCommentService.checkComment(commentId);
        Event event = checkEventService.checkEvent(eventId);
        if (!comment.getEvent().getId().equals(event.getId())) {
            throw new ValidationException("Некорректный запрос");
        }
        return logAndReturn(
                commentMapper.toDto(comment),
                com -> log.info("Retrieved comment with id={} (admin view)", commentId)
        );
    }

    @Override
    @Transactional
    public CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        return logAndReturn(commentMapper.toDto(commentRepository.save(Comment.builder()
                        .text(commentCreateDto.getText())
                        .created(commentCreateDto.getCreated())
                        .updated(commentCreateDto.getUpdated())
                        .updatedBy(commentCreateDto.getUpdatedBy())
                        .author(checkUserService.checkUser(userId))
                        .event(checkEventService.checkEvent(eventId))
                        .build())),
                savedComment -> log.info("Comment created successfully: {}",
                        savedComment)
        );
    }

    @Override
    @Transactional
    public CommentDto updateCommentByAdmin(CommentUpdateDto updateDto, Long commentId) {
        Comment comment = checkCommentService.checkComment(commentId);
        comment.setText(updateDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedBy(UpdatedBy.ADMIN.toString());
        return logAndReturn(
                commentMapper.toDto(commentRepository.save(comment)),
                updatedComment -> log.info("Comment with id={} updated by admin", commentId)
        );
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = checkCommentService.checkComment(commentId);
        commentRepository.delete(comment);
        log.info("Comment with id={} deleted by admin", commentId);
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long commentId, Long userId, Long eventId,CommentUpdateDto updateDto) {
        Comment comment = checkCommentService.checkComment(commentId);
        Event event = checkEventService.checkEvent(eventId);
        if (!comment.getEvent().getId().equals(event.getId())) {
            throw new ValidationException("Некорректный запрос");
        }
        checkUserIsAuthor(comment, userId);
        checkUpdatedByAdmin(comment);
        comment.setText(updateDto.getText());
        comment.setUpdated(updateDto.getUpdated());
        return logAndReturn(
                commentMapper.toDto(commentRepository.save(comment)),
                updatedComment -> log.info("Comment {} updated by user {}", commentId, userId)
        );
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long commentId, Long userId,Long eventId) {
        Comment comment = checkCommentService.checkComment(commentId);
        checkUserIsAuthor(comment, userId);
        Event event = checkEventService.checkEvent(eventId);
        if (!comment.getEvent().getId().equals(event.getId())) {
            throw new ValidationException(String.format("User with id=%d isnt an author of comment with id=%d",
                    userId, commentId));
        }
        commentRepository.delete(comment);
        log.info("Comment {} deleted by user {}", commentId, userId);
    }

    private void checkUserIsAuthor(Comment comment, Long userId) {
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("User " + userId + " is not author of comment " + comment.getId());
        }
    }

    private void checkUpdatedByAdmin(Comment comment) {
        if (comment.getUpdatedBy() != null && comment.getUpdatedBy().equals(UpdatedBy.ADMIN.toString())) {
            throw new ForbiddenException("You are not allowed to update this comment");
        }
    }
}