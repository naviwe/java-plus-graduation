package ewm.event.mapper;

import ewm.event.Event;
import ewm.interaction.client.UserFeignClient;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, LocationMapper.class})
public abstract class EventMapper {

    private UserFeignClient userFeignClient;

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    @Mapping(target = "initiator", expression = "java(getUserShortDto(event.getInitiatorId()))")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "eventDate", source = "eventDate")
    public abstract EventFullDto toFullDto(Event event);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    @Mapping(target = "initiator", expression = "java(getUserShortDto(event.getInitiatorId()))")
    @Mapping(target = "eventDate", source = "eventDate")
    public abstract EventShortDto toShortDto(Event event);

    public abstract List<EventShortDto> toShortDtoList(List<Event> events);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "newEventDto.category", qualifiedByName = "mapCategoryFromId")
    @Mapping(target = "initiatorId", source = "userId")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "eventDate", source = "newEventDto.eventDate", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "annotation", source = "newEventDto.annotation")
    @Mapping(target = "description", source = "newEventDto.description")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "paid", source = "newEventDto.paid")
    @Mapping(target = "participantLimit", source = "newEventDto.participantLimit")
    @Mapping(target = "requestModeration", source = "newEventDto.requestModeration")
    @Mapping(target = "title", source = "newEventDto.title")
    public abstract Event toEvent(NewEventDto newEventDto, Long userId);

    @Named("mapCategory")
    public CategoryDto mapCategory(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }

    @Named("mapCategoryFromId")
    public Category mapCategoryFromId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryId);
        return category;
    }

    @Named("stringToLocalDateTime")
    public LocalDateTime stringToLocalDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTime, formatter);
    }

    public UserShortDto getUserShortDto(Long userId) {
        Map<Long, UserShortDto> usersMap = userFeignClient.getUsersByIDS(List.of(userId));
        return usersMap.get(userId);
    }

    public Map<Long, UserShortDto> getUsersMap(List<Long> userIds) {
        return userFeignClient.getUsersByIDS(userIds);
    }

    @Autowired
    public void setUserFeignClient(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }
}