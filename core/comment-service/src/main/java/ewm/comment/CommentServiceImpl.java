package ewm.comment;

import ewm.interaction.dto.comment.CommentCreateDto;
import ewm.interaction.dto.comment.CommentDto;
import ewm.interaction.dto.comment.CommentUpdateDto;
import ewm.interaction.dto.comment.UpdatedBy;
import ewm.interaction.exception.ForbiddenException;
import ewm.interaction.exception.ValidationException;
import ewm.interaction.utils.CheckEventService;
import ewm.interaction.utils.CheckUserService;
import ewm.interaction.utils.LoggingUtils;
import ewm.utils.CheckCommentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        checkEventService.checkEvent(eventId);
        Pageable pageRequest = PageRequest.of(from / size, size);
        Page<Comment> commentPage = commentRepository.findByEventId(eventId, pageRequest);
        return LoggingUtils.logAndReturn(commentPage.getContent()
                        .stream()
                        .map(commentMapper::toDto)
                        .toList(),
                comments -> log.info("Found {} comments for event with id= {}", comments.size(), eventId)
        );
    }

    @Override
    public CommentDto getComment(Long commentId, Long eventId) {
        Comment comment = checkCommentService.checkComment(commentId);
        checkEventService.checkEvent(eventId);
        if (!comment.getEventId().equals(eventId)) {
            throw new ValidationException("Comment does not belong to the specified event");
        }
        return LoggingUtils.logAndReturn(
                commentMapper.toDto(comment),
                com -> log.info("Retrieved comment with id={} (admin view)", commentId)
        );
    }

    @Override
    @Transactional
    public CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        checkUserService.checkUser(userId);
        checkEventService.checkEvent(eventId);
        if (!commentCreateDto.getEventId().equals(eventId)) {
            throw new ValidationException("Event ID in request body does not match the path variable");
        }

        Comment comment = commentMapper.toEntity(commentCreateDto);
        comment.setAuthorId(userId);
        comment.setEventId(eventId);

        return LoggingUtils.logAndReturn(commentMapper.toDto(commentRepository.save(comment)),
                savedComment -> log.info("Comment created successfully: {}", savedComment)
        );
    }

    @Override
    @Transactional
    public CommentDto updateCommentByAdmin(CommentUpdateDto updateDto, Long commentId) {
        Comment comment = checkCommentService.checkComment(commentId);
        comment.setText(updateDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedBy(UpdatedBy.ADMIN.toString());
        return LoggingUtils.logAndReturn(
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
    public CommentDto updateCommentByUser(Long commentId, Long userId, Long eventId, CommentUpdateDto updateDto) {
        Comment comment = checkCommentService.checkComment(commentId);
        checkEventService.checkEvent(eventId);
        if (!comment.getEventId().equals(eventId)) {
            throw new ValidationException("Comment does not belong to the specified event");
        }
        checkUserIsAuthor(comment, userId);
        checkUpdatedByAdmin(comment);
        comment.setText(updateDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedBy(UpdatedBy.USER.toString());
        return LoggingUtils.logAndReturn(
                commentMapper.toDto(commentRepository.save(comment)),
                updatedComment -> log.info("Comment {} updated by user {}", commentId, userId)
        );
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long commentId, Long userId, Long eventId) {
        Comment comment = checkCommentService.checkComment(commentId);
        checkUserIsAuthor(comment, userId);
        checkEventService.checkEvent(eventId);
        if (!comment.getEventId().equals(eventId)) {
            throw new ValidationException("Comment does not belong to the specified event");
        }
        commentRepository.delete(comment);
        log.info("Comment {} deleted by user {}", commentId, userId);
    }

    private void checkUserIsAuthor(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("User " + userId + " is not author of comment " + comment.getId());
        }
    }

    private void checkUpdatedByAdmin(Comment comment) {
        if (comment.getUpdatedBy() != null && comment.getUpdatedBy().equals(UpdatedBy.ADMIN.toString())) {
            throw new ForbiddenException("You are not allowed to update this comment");
        }
    }
}