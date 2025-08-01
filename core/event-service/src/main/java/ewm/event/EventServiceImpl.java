package ewm.event;

import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.event.*;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.exception.ConflictException;
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
import ewm.event.mapper.EventMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import ru.practicum.ewm.stats.client.RecommendationClient;

import static ewm.interaction.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {
    EventRepository eventRepository;
    UserFeignClient userClient;
    EventValidationService eventValidationService;
    EventMapper eventMapper;
    CheckCategoryService checkCategoryService;
    LocationRepository locationRepository;
    RecommendationClient recommendationClient;

    @Override
    public List<EventShortDto> findEventsByInitiatorId(Long userId, Integer from, Integer size) {
        UserDto userDto = userClient.getUser(userId);
        Pageable pageRequest = PageRequest.of(from / size, size);
        Page<Event> eventPage = eventRepository.findByInitiatorId(userId, pageRequest);
        List<Long> eventIds = eventPage.getContent().stream().map(Event::getId).toList();
        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(eventIds);
        return logAndReturn(eventPage.getContent()
                        .stream()
                        .map(event -> {
                            EventShortDto dto = eventMapper.toShortDto(event, UserShortDto.builder()
                                    .id(userDto.getId())
                                    .name(userDto.getName()).build());
                            dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
                            return dto;
                        })
                        .toList(),
                events -> log.info("Found {} events for user with id={}", events.size(), userId)
        );
    }

    @Override
    public EventFullDto findById(Long userId, Long eventId) {
        UserDto userDto = userClient.getUser(userId);
        if (!eventRepository.findByInitiatorId(userId).equals(eventRepository.findById(eventId).get())) {
            throw new ConflictException(String.format("User with id=%d isnt a initiator for event with id=%d",
                    userId, eventId));
        }
        return logAndReturn(eventMapper.toFullDto(eventValidationService.checkEvent(eventId),UserShortDto.builder()
                        .id(userDto.getId())
                        .name(userDto.getName()).build()),
                event -> log.info("Found event with id={}",
                        event.getId())
        );
    }

    @Override
    @Transactional
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        Event event = eventMapper.toEvent(newEventDto);
        Location location = event.getLocation();
        if (location != null) {
            location = locationRepository.save(location);
            event.setLocation(location);
        }
        UserDto userDto = userClient.getUser(userId);
        event.setInitiatorId(userDto.getId());
        event.setCategory(checkCategoryService.checkCategory(newEventDto.getCategory()));
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);
        event.setConfirmedRequests(0L);
        event.setRating(0.0);
        Event savedEvent = eventRepository.save(event);
        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(List.of(savedEvent.getId()));
        EventFullDto eventFullDto = eventMapper.toFullDto(savedEvent, UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName()).build());
        eventFullDto.setRating(ratings.getOrDefault(savedEvent.getId(), 0.0));
        return logAndReturn(eventFullDto, dto -> log.info("Event created successfully: {}", dto));
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long userId, Long eventId) {
        UserDto userDto = userClient.getUser(userId);
        Event event = eventValidationService.checkEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d isn't a initiator for event with id=%d", userId, eventId));
        }
        if (event.getState() == State.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
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
            Location location = new Location();
            location.setLat(updateEventRequest.getLocation().getLat());
            location.setLon(updateEventRequest.getLocation().getLon());
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
        if (updateEventRequest.getStateAction() != null) {
            switch (updateEventRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(State.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(State.CANCELED);
                    break;
                default:
                    throw new IllegalArgumentException("Неподдерживаемое действие: " + updateEventRequest.getStateAction());
            }
        }
        Event savedEvent = eventRepository.save(event);
        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(List.of(savedEvent.getId()));
        EventFullDto eventFullDto = eventMapper.toFullDto(savedEvent, UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName()).build());
        eventFullDto.setRating(ratings.getOrDefault(savedEvent.getId(), 0.0));
        return logAndReturn(eventFullDto, dto -> log.info("Event updated successfully: {}", dto));
    }
}