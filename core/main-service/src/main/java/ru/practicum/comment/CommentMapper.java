package ru.practicum.comment;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.comment.dto.CommentCreateDto;
import ru.practicum.comment.dto.CommentDto;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    Comment toEntity(CommentCreateDto commentCreateDto);

    @Mapping(target = "author", source = "author.id")
    @Mapping(target = "event", source = "event.id")
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
