package ewm.comment;

import ewm.interaction.feign.EventFeignClient;
import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.comment.CommentCreateDto;
import ewm.interaction.dto.comment.CommentDto;
import ewm.interaction.dto.comment.CommentUpdateDto;
import ewm.interaction.dto.comment.UpdatedBy;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.exception.ForbiddenException;
import ewm.interaction.exception.NotFoundException;
import ewm.interaction.exception.ValidationException;
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

import static ewm.interaction.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImpl implements CommentService {
    UserFeignClient userClient;
    CommentRepository commentRepository;
    CommentMapper commentMapper;
    EventFeignClient eventClient;

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
    public CommentDto getComment(Long commentId, Long eventId) {
        Comment comment = getCommentById(commentId);
        EventFullDto event = eventClient.getEventByIdInternal(eventId);
        if (!comment.getEventId().equals(event.getId())) {
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
        Comment comment = commentMapper.toEntity(commentCreateDto);
        comment.setAuthorId(userClient.getUser(userId).getId());
        comment.setEventId(eventClient.getEventByIdInternal(eventId).getId());

        return logAndReturn(
                commentMapper.toDto(commentRepository.save(comment)),
                savedComment -> log.info("Comment created successfully: {}", savedComment)
        );
    }

    @Override
    @Transactional
    public CommentDto updateCommentByAdmin(CommentUpdateDto updateDto, Long commentId) {
        Comment comment = getCommentById(commentId);
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
        Comment comment = getCommentById(commentId);
        commentRepository.delete(comment);
        log.info("Comment with id={} deleted by admin", commentId);
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long commentId, Long userId, Long eventId, CommentUpdateDto updateDto) {
        Comment comment = getCommentById(commentId);
        EventFullDto event = eventClient.getEventByIdInternal(eventId);
        if (!comment.getEventId().equals(event.getId())) {
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
    public void deleteCommentByUser(Long commentId, Long userId, Long eventId) {
        Comment comment = getCommentById(commentId);
        checkUserIsAuthor(comment, userId);
        EventFullDto event = eventClient.getEventByIdInternal(eventId);
        if (!comment.getEventId().equals(event.getId())) {
            throw new ValidationException(String.format("User with id=%d isnt an author of comment with id=%d",
                    userId, commentId));
        }
        commentRepository.delete(comment);
        log.info("Comment {} deleted by user {}", commentId, userId);
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Comment with id=%d was not found", commentId)));
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