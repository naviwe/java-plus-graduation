package ewm.comment;

import ewm.interaction.dto.comment.CommentCreateDto;
import ewm.interaction.dto.comment.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", source = "commentCreateDto.eventId")
    Comment toEntity(CommentCreateDto commentCreateDto);

    @Mapping(target = "author", source = "authorId")
    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "timestamp", source = "comment", qualifiedByName = "mapTimestamp")
    @Mapping(target = "updated", source = "comment", qualifiedByName = "mapUpdated")
    CommentDto toDto(Comment comment);

    @Named("mapTimestamp")
    default LocalDateTime mapTimestamp(Comment comment) {
        return comment.getUpdated();
    }

    @Named("mapUpdated")
    default Boolean mapUpdated(Comment comment) {
        return !comment.getUpdated().equals(comment.getCreated());
    }
}