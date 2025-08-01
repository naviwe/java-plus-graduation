package ewm.event.pub;

import ewm.event.Event;
import ewm.event.EventRepository;
import ewm.event.mapper.EventMapper;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.State;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.exception.NotFoundException;
import ewm.interaction.exception.ValidationException;
import ewm.utils.EventValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.client.RecommendationClient;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventPublicServiceImpl implements EventPublicService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    EventValidationService eventValidationService;
    UserFeignClient userClient;
    CollectorClient collectorClient;
    RecommendationClient recommendationClient;

    public EventPublicServiceImpl(EventRepository eventRepository,
                                  EventMapper eventMapper,
                                  EventValidationService eventValidationService,
                                  UserFeignClient userClient,
                                  CollectorClient collectorClient,
                                  RecommendationClient recommendationClient) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.eventValidationService = eventValidationService;
        this.userClient = userClient;
        this.collectorClient = collectorClient;
        this.recommendationClient = recommendationClient;
    }

    static LocalDateTime minTime = LocalDateTime.of(1970, 1, 1, 0, 0);
    static LocalDateTime maxTime = LocalDateTime.of(3000, 1, 1, 0, 0);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                         String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                         String sort, Integer from, Integer size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(from / size, size);

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : minTime;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : maxTime;
        text = text != null ? text : "";
        Page<Event> events = eventRepository.findEvents(text, paid, start, end, categories, onlyAvailable,
                State.PUBLISHED, pageable);
        if (events.isEmpty()) {
            throw new ValidationException("Нет подходящих событий");
        }

        List<Long> userIds = events.stream().map(Event::getInitiatorId).toList();
        List<UserShortDto> usersDto = userClient.getUsers(userIds, 0, userIds.size()).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .toList();

        List<EventShortDto> dtos = events.map(event -> eventMapper.toShortDto(event, usersDto.stream()
                .filter(userShortDto -> userShortDto.getId().equals(event.getInitiatorId())).findAny()
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет")))).toList();

        List<Long> eventIds = dtos.stream().map(EventShortDto::getId).collect(Collectors.toList());
        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(eventIds);
        dtos.forEach(dto -> dto.setRating(ratings.getOrDefault(dto.getId(), 0.0)));

        if (sort != null) {
            if (sort.equals("EVENT_DATE")) {
                dtos = dtos.stream()
                        .sorted(Comparator.comparing(EventShortDto::getEventDate))
                        .collect(Collectors.toList());
            } else if (sort.equals("RATING")) {
                dtos = dtos.stream()
                        .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.reverseOrder()))
                        .collect(Collectors.toList());
            }
        }

        return dtos;
    }

    @Override
    public EventFullDto getEventById(Long id, HttpServletRequest request, @RequestHeader("X-EWM-USER-ID") Long userId) {
        Event event = eventValidationService.checkPublishedEvent(id);
        UserDto userDto = userClient.getUser(event.getInitiatorId());
        EventFullDto eventFullDto = eventMapper.toFullDto(event, UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build());

        collectorClient.viewEvent(userId, id);

        Map<Long, Double> ratings = recommendationClient.getInteractionsCount(List.of(id));
        eventFullDto.setRating(ratings.getOrDefault(id, 0.0));

        return eventFullDto;
    }

    @Override
    public EventFullDto getEventByIdInternal(Long id) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Ивента с таким id нет"));
        UserDto userDto = userClient.getUser(event.getInitiatorId());
        return eventMapper.toFullDto(event, UserShortDto.builder().id(userDto.getId())
                .name(userDto.getName()).build());
    }

    @Override
    public void changeEventFields(EventFullDto eventFullDto) {
        Event event = eventRepository.findById(eventFullDto.getId())
                .orElseThrow(() -> new NotFoundException("События с таким id нет"));
        if (!eventFullDto.getConfirmedRequests().equals(event.getConfirmedRequests())) {
            event.setConfirmedRequests(eventFullDto.getConfirmedRequests());
        }
        eventRepository.save(event);
    }

    @Override
    public List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId, Integer from, Integer size) {
        List<RecommendedEventProto> recommendations = recommendationClient.getRecommendations(userId, size)
                .toList();

        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> events = eventRepository.findEvents(null, null, minTime, maxTime, null, null, State.PUBLISHED, pageable);

        List<Long> userIds = events.stream().map(Event::getInitiatorId).toList();
        List<UserShortDto> usersDto = userClient.getUsers(userIds, 0, userIds.size()).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .toList();

        List<EventShortDto> dtos = events.map(event -> eventMapper.toShortDto(event, usersDto.stream()
                .filter(userShortDto -> userShortDto.getId().equals(event.getInitiatorId())).findAny()
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет")))).toList();

        Map<Long, Double> ratings = recommendations.stream()
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
        dtos.forEach(dto -> dto.setRating(ratings.getOrDefault(dto.getId(), 0.0)));

        return dtos.stream()
                .filter(dto -> eventIds.contains(dto.getId()))
                .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public void likeEvent(Long eventId, @RequestHeader("X-EWM-USER-ID") Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("События с таким id нет"));
        boolean hasVisited = recommendationClient.getInteractionsCount(List.of(eventId))
                .entrySet().stream()
                .anyMatch(entry -> entry.getKey().equals(eventId) && entry.getValue() > 0);
        if (!hasVisited) {
            throw new ValidationException("Пользователь не посещал мероприятие с id=" + eventId);
        }

        collectorClient.addLikeEvent(userId, eventId);
    }
}