package ewm.event.mapper;

import ewm.event.Event;

import ewm.interaction.dto.event.CategoryDto;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.NewEventDto;
import ewm.interaction.dto.user.UserShortDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ewm.category.mapper.CategoryMapper;
import ewm.category.model.Category;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "category", source = "event.category", qualifiedByName = "mapCategory")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "eventDate", source = "event.eventDate")
    @Mapping(target = "id", source = "event.id")
    EventFullDto toFullDto(Event event, UserShortDto initiator);

    @Mapping(target = "category", source = "event.category", qualifiedByName = "mapCategory")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "eventDate", source = "event.eventDate")
    @Mapping(target = "id", source = "event.id")
    EventShortDto toShortDto(Event event, UserShortDto initiator);

    List<EventShortDto> toShortDtoList(List<Event> events);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryFromId")
    @Mapping(target = "initiatorId", ignore = true)
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