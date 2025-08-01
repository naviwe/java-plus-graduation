package ewm.event.admin;

import ewm.event.*;
import ewm.event.mapper.EventMapper;
import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.State;
import ewm.interaction.dto.event.StateAction;
import ewm.interaction.dto.event.UpdateEventRequest;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.exception.ForbiddenException;
import ewm.interaction.exception.NotFoundException;
import ewm.interaction.utils.LoggingUtils;
import ewm.utils.CheckCategoryService;
import ewm.utils.EventValidationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.category.model.Category;
import ru.practicum.ewm.stats.client.RecommendationClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventAdminServiceImpl implements EventAdminService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    LocationRepository locationRepository;
    UserFeignClient userClient;
    EventValidationService eventValidationService;
    CheckCategoryService checkCategoryService;
    RecommendationClient recommendationClient;

    static LocalDateTime minTime = LocalDateTime.of(1970, 1, 1, 0, 0);
    static LocalDateTime maxTime = LocalDateTime.of(3000, 1, 1, 0, 0);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventFullDto> getEvents(List<Long> userIds, List<String> statesStr,
                                        List<Long> categories, String rangeStart,
                                        String rangeEnd, Integer from, Integer size) {
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : minTime;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : maxTime;

        List<State> states = statesStr != null ? statesStr.stream().map(State::valueOf).toList() : null;

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> events = eventRepository.findAllEventsByAdmin(userIds, states, categories, start, end, pageable);
        List<Long> eventIds = events.getContent().stream().map(Event::getId).toList();
        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(eventIds);

        List<Long> userFromClientIds = events.stream().map(Event::getInitiatorId).toList();
        List<UserShortDto> usersDto = userClient.getUsers(userFromClientIds, 0, userFromClientIds.size()).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .toList();

        return events.map(event -> {
            EventFullDto dto = eventMapper.toFullDto(event, usersDto.stream()
                    .filter(userShortDto -> userShortDto.getId().equals(event.getInitiatorId())).findAny()
                    .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет")));
            dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
            return dto;
        }).toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long eventId) {
        if (updateEventRequest == null) {
            throw new ForbiddenException("Cannot publish the event: request body is missing.");
        }
        Event event = eventValidationService.checkEvent(eventId);

        if (event.getState().equals(State.PUBLISHED) || event.getState().equals(State.CANCELED)) {
            throw new ConflictException("Only pending events can be published");
        }

        if (updateEventRequest.getStateAction() != null) {
            if (updateEventRequest.getStateAction().equals(StateAction.PUBLISH_EVENT)) {
                event.setState(State.PUBLISHED);
            } else {
                throw new ForbiddenException("Cannot publish the event because it's not in the right state: PUBLISHED");
            }
        }
        if (updateEventRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventRequest.getAnnotation());
        }
        if (updateEventRequest.getCategory() != null) {
            Category category = checkCategoryService.checkCategory(updateEventRequest.getCategory());
            category.setId(updateEventRequest.getCategory());
            event.setCategory(category);
        }
        if (updateEventRequest.getDescription() != null) {
            event.setDescription(updateEventRequest.getDescription());
        }
        if (updateEventRequest.getEventDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime eventDate = LocalDateTime.parse(updateEventRequest.getEventDate(), formatter);
            event.setEventDate(eventDate);
        }
        if (updateEventRequest.getLocation() != null) {
            Double lat = updateEventRequest.getLocation().getLat();
            Double lon = updateEventRequest.getLocation().getLon();
            Location location = locationRepository.findByLatAndLon(lat, updateEventRequest.getLocation().getLon())
                    .orElseGet(() -> locationRepository.save(Location.builder().lon(lon).lat(lat).build()));
            location.setLat(lat);
            location.setLon(lon);
            event.setLocation(location);
        }
        if (updateEventRequest.getPaid() != null) {
            event.setPaid(updateEventRequest.getPaid());
        }
        if (updateEventRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventRequest.getParticipantLimit());
        }
        if (updateEventRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventRequest.getRequestModeration());
        }
        if (updateEventRequest.getTitle() != null) {
            event.setTitle(updateEventRequest.getTitle());
        }
        Event savedEvent = eventRepository.save(event);
        UserDto userDto = userClient.getUser(savedEvent.getInitiatorId());
        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(List.of(savedEvent.getId()));
        EventFullDto eventFullDto = eventMapper.toFullDto(savedEvent, UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName()).build());
        eventFullDto.setRating(ratings.getOrDefault(savedEvent.getId(), 0.0));
        return LoggingUtils.logAndReturn(eventFullDto, dto -> log.info("Event updated successfully: {}", dto));
    }
}