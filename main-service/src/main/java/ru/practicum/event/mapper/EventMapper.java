package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.Event;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.user.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class, LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "eventDate", source = "eventDate")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "eventDate", source = "eventDate")
    EventShortDto toShortDto(Event event);

    List<EventShortDto> toShortDtoList(List<Event> events);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryFromId")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "eventDate", source = "eventDate", qualifiedByName = "stringToLocalDateTime")
    Event toEvent(NewEventDto newEventDto);

    @Named("mapCategory")
    default CategoryDto mapCategory(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }

    @Named("mapCategoryFromId")
    default Category mapCategoryFromId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryId);
        return category;
    }

    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTime, formatter);
    }
}