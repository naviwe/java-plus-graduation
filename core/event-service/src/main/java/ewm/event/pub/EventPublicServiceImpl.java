package ewm.event.pub;

import ewm.event.Event;
import ewm.event.EventRepository;
import ewm.event.mapper.EventMapper;
import ewm.interaction.dto.request.RequestStatus;
import ewm.interaction.feign.RequestFeignClient;
import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.State;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.exception.NotFoundException;
import ewm.interaction.exception.ValidationException;
import ewm.utils.EventValidationService;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.client.AnalyzerClient;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventPublicServiceImpl implements EventPublicService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    EventValidationService eventValidationService;
    UserFeignClient userClient;
    AnalyzerClient analyzerClient;
    CollectorClient collectorClient;
    RequestFeignClient requestFeignClient;

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                         String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                         String sort, Integer from, Integer size, HttpServletRequest request) {
        if (rangeStart != null && rangeEnd != null) {
            LocalDateTime start = LocalDateTime.parse(rangeStart, formatter);
            LocalDateTime end = LocalDateTime.parse(rangeEnd, formatter);
            if (end.isBefore(start)) {
                throw new ValidationException("End date cannot be earlier than start date");
            }
        }

        Sort sortOption = sort != null && sort.equals("EVENT_DATE") ?
                Sort.by("eventDate").ascending() :
                Sort.by("rating").descending();
        Pageable pageable = PageRequest.of(from / size, size, sortOption);

        LocalDateTime start = rangeStart != null ?
                LocalDateTime.parse(rangeStart, formatter) :
                LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ?
                LocalDateTime.parse(rangeEnd, formatter) :
                LocalDateTime.now().plusYears(100);

        text = text != null ? text.toLowerCase() : "";

        Page<Event> events = eventRepository.findEvents(
                text, paid, start, end, categories, onlyAvailable, State.PUBLISHED, pageable);

        if (events.isEmpty()) {
            log.info("No events found for parameters: text={}, categories={}, paid={}, start={}, end={}, onlyAvailable={}",
                    text, categories, paid, start, end, onlyAvailable);
            return List.of();
        }

        List<Long> eventIds = events.getContent().stream().map(Event::getId).toList();
        Map<Long, Double> ratings;
        try {
            ratings = analyzerClient.getInteractionsCount(eventIds);
        } catch (StatusRuntimeException e) {
            log.error("Failed to retrieve event ratings from analyzer: {}", e.getStatus(), e);
            ratings = Map.of();
        }

        List<Long> userIds = events.getContent().stream().map(Event::getInitiatorId).toList();
        List<UserShortDto> usersDto = userClient.getUsers(userIds, 0, userIds.size()).stream()
                .map(userDto -> UserShortDto.builder()
                        .id(userDto.getId())
                        .name(userDto.getName())
                        .build())
                .toList();

        Map<Long, Double> finalRatings = ratings;
        return events.getContent().stream().map(event -> {
            UserShortDto initiator = usersDto.stream()
                    .filter(user -> user.getId().equals(event.getInitiatorId()))
                    .findAny()
                    .orElseThrow(() -> new NotFoundException("User with id=" + event.getInitiatorId() + " not found"));
            double rating = finalRatings.getOrDefault(event.getId(), 0.0);
            event.setRating(rating);
            return eventMapper.toShortDto(event, initiator);
        }).toList();
    }

    @Override
    public EventFullDto getEventById(Long id, HttpServletRequest request, Long userId) {
        Event event = eventValidationService.checkPublishedEvent(id);
        return getEventFullDto(event, userId);
    }

    @Override
    public EventFullDto getEventByIdInternal(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " not found"));
        return getEventFullDto(event, null);
    }

    @Override
    public void changeEventFields(EventFullDto eventFullDto) {
        Event event = eventRepository.findById(eventFullDto.getId())
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventFullDto.getId() + " not found"));

        if (!eventFullDto.getConfirmedRequests().equals(event.getConfirmedRequests())) {
            event.setConfirmedRequests(eventFullDto.getConfirmedRequests());
            eventRepository.save(event);
            log.info("Updated confirmed requests for event id={} to {}", event.getId(), event.getConfirmedRequests());
        }
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, Integer from, Integer size) {
        List<RecommendedEventProto> recommendedEvents;
        try {
            recommendedEvents = analyzerClient.getRecommendations(userId, size)
                    .skip(from)
                    .limit(size)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch recommendations for userId={}: {}", userId, e.getStatus(), e);
            return List.of();
        }

        if (recommendedEvents.isEmpty()) {
            log.info("No recommendations available for userId={}", userId);
            return List.of();
        }

        List<Long> eventIds = recommendedEvents.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        List<Event> events = eventRepository.findAllById(eventIds).stream()
                .filter(event -> event.getState() == State.PUBLISHED)
                .toList();

        if (events.isEmpty()) {
            log.info("No published events found for recommended event IDs: {}", eventIds);
            return List.of();
        }

        List<Long> userIds = events.stream().map(Event::getInitiatorId).toList();
        List<UserShortDto> usersDto = userClient.getUsers(userIds, 0, userIds.size()).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .toList();

        Map<Long, Double> ratings;
        try {
            ratings = analyzerClient.getInteractionsCount(eventIds);
        } catch (StatusRuntimeException e) {
            log.error("Failed to retrieve ratings for event IDs {}: {}", eventIds, e.getStatus(), e);
            ratings = Map.of();
        }

        Map<Long, Double> finalRatings = ratings;
        return events.stream().map(event -> {
            UserShortDto initiator = usersDto.stream()
                    .filter(user -> user.getId().equals(event.getInitiatorId()))
                    .findAny()
                    .orElseThrow(() -> new NotFoundException("User with id=" + event.getInitiatorId() + " not found"));
            double rating = finalRatings.getOrDefault(event.getId(), 0.0);
            event.setRating(rating);
            return eventMapper.toShortDto(event, initiator);
        }).toList();
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        Event event = eventValidationService.checkPublishedEvent(eventId);
        userClient.getUser(userId);

        boolean hasConfirmedRequest = requestFeignClient.findRequestsByEventId(userId, eventId)
                .stream()
                .anyMatch(req -> req.getEvent().equals(eventId) && req.getStatus().equals(RequestStatus.CONFIRMED.toString()));

        if (!hasConfirmedRequest) {
            log.warn("User id={} is not registered for event id={}", userId, eventId);
            throw new ValidationException("User is not registered for event with id=" + eventId);
        }

        try {
            log.info("Recording LIKE from userId={} for eventId={}", userId, eventId);
            collectorClient.addLikeEvent(userId, eventId);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error while recording LIKE for eventId={}: {}", eventId, e.getStatus(), e);
            throw new ValidationException("Error while processing like for event with id=" + eventId);
        }
    }

    private EventFullDto getEventFullDto(Event event, Long userId) {
        Map<Long, Double> ratings;
        try {
            ratings = analyzerClient.getInteractionsCount(List.of(event.getId()));
        } catch (StatusRuntimeException e) {
            log.error("Failed to retrieve rating for eventId={}: {}", event.getId(), e.getStatus(), e);
            ratings = Map.of();
        }

        double rating = ratings.getOrDefault(event.getId(), 0.0);
        event.setRating(rating);

        if (userId != null) {
            try {
                collectorClient.viewEvent(userId, event.getId());
                log.info("Recorded VIEW from userId={} for eventId={}", userId, event.getId());
            } catch (StatusRuntimeException e) {
                log.error("gRPC error while recording VIEW for eventId={}: {}", event.getId(), e.getStatus(), e);
            }
        }

        UserShortDto initiator = userClient.getUsers(List.of(event.getInitiatorId()), 0, 1).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("User with id=" + event.getInitiatorId() + " not found"));

        return eventMapper.toFullDto(event, initiator);
    }
}