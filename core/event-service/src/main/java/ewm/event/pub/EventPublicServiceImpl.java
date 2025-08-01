package ewm.event.pub;

import ewm.event.Event;
import ewm.event.EventRepository;
import ewm.event.mapper.EventMapper;
import ewm.interaction.dto.request.ParticipationRequestDto;
import ewm.interaction.dto.request.RequestStatus;
import ewm.interaction.feign.RequestFeignClient;
import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.State;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.exception.NotFoundException;
import ewm.interaction.exception.ValidationException;
import ewm.utils.EventValidationService;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.client.AnalyzerClient;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    AnalyzerClient analyzerClient;
    CollectorClient collectorClient;
    RequestFeignClient requestFeignClient;

    public EventPublicServiceImpl(EventRepository eventRepository,
                                  EventMapper eventMapper,
                                  EventValidationService eventValidationService,
                                  UserFeignClient userClient,
                                  AnalyzerClient analyzerClient,
                                  CollectorClient collectorClient, RequestFeignClient requestFeignClient) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.eventValidationService = eventValidationService;
        this.userClient = userClient;
        this.analyzerClient = analyzerClient;
        this.collectorClient = collectorClient;
        this.requestFeignClient = requestFeignClient;
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
        text = text != null ? text.toLowerCase() : "";

        Page<Event> events = eventRepository.findEvents(text, paid, start, end, categories, onlyAvailable,
                State.PUBLISHED, pageable);

        if (events.isEmpty()) {
            throw new ValidationException("Нет подходящих событий");
        }

        List<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toList());
        List<UserShortDto> usersDto = userClient.getUsers(userIds, 0, userIds.size()).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .collect(Collectors.toList());

        List<Long> eventIds = events.getContent().stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);

        List<EventShortDto> dtos = events.getContent().stream().map(event -> {
            UserShortDto initiator = usersDto.stream()
                    .filter(user -> user.getId().equals(event.getInitiatorId()))
                    .findAny()
                    .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет"));
            double rating = ratings.getOrDefault(event.getId(), 0.0);
            event.setRating(rating);
            return eventMapper.toShortDto(event, initiator);
        }).collect(Collectors.toList());

        if (sort != null) {
            if (sort.equals("EVENT_DATE")) {
                dtos = dtos.stream()
                        .sorted((dto1, dto2) -> dto1.getEventDate().compareTo(dto2.getEventDate()))
                        .collect(Collectors.toList());
            } else if (sort.equals("RATING")) {
                dtos = dtos.stream()
                        .sorted((dto1, dto2) -> Double.compare(dto2.getRating(), dto1.getRating()))
                        .collect(Collectors.toList());
            }
        }

        return dtos;
    }

    @Override
    public EventFullDto getEventById(Long id, HttpServletRequest request, Long userId) {
        Event event = eventValidationService.checkPublishedEvent(id);
        UserDto userDto = userClient.getUser(event.getInitiatorId());

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(id));
        double rating = ratings.getOrDefault(id, 0.0);
        event.setRating(rating);

        collectorClient.viewEvent(userId, id);

        return eventMapper.toFullDto(event, UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build());
    }

    @Override
    public EventFullDto getEventByIdInternal(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ивента с таким id нет"));
        UserDto userDto = userClient.getUser(event.getInitiatorId());

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(id));
        double rating = ratings.getOrDefault(id, 0.0);
        event.setRating(rating);

        return eventMapper.toFullDto(event, UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build());
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
    public List<EventShortDto> getRecommendations(Long userId, Integer from, Integer size) {
        List<RecommendedEventProto> recommendedEvents = analyzerClient.getRecommendations(userId, size)
                .skip(from)
                .limit(size)
                .toList();

        if (recommendedEvents.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = recommendedEvents.stream()
                .map(RecommendedEventProto::getEventId)
                .collect(Collectors.toList());

        List<Event> events = eventRepository.findAllById(eventIds).stream()
                .filter(event -> event.getState() == State.PUBLISHED)
                .toList();

        List<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toList());
        List<UserShortDto> usersDto = userClient.getUsers(userIds, 0, userIds.size()).stream()
                .map(userDto -> UserShortDto.builder().id(userDto.getId()).name(userDto.getName()).build())
                .toList();

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);

        return events.stream().map(event -> {
            UserShortDto initiator = usersDto.stream()
                    .filter(user -> user.getId().equals(event.getInitiatorId()))
                    .findAny()
                    .orElseThrow(() -> new NotFoundException("Пользователя с таким id нет"));
            double rating = ratings.getOrDefault(event.getId(), 0.0);
            event.setRating(rating);
            return eventMapper.toShortDto(event, initiator);
        }).collect(Collectors.toList());
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        Event event = eventValidationService.checkPublishedEvent(eventId);

        List<ParticipationRequestDto> requests = requestFeignClient.findRequestsByUserId(userId);
        boolean hasRegistered = requests.stream()
                .anyMatch(req -> req.getEvent().equals(eventId) && req.getStatus().equals(RequestStatus.CONFIRMED.toString()));
        if (!hasRegistered) {
            log.warn("User {} has not confirmed registration for event {}", userId, eventId);
            throw new ValidationException("Пользователь не зарегистрирован на мероприятие с id=" + eventId);
        }

        try {
            log.info("Recording like for user {} and event {}", userId, eventId);
            collectorClient.addLikeEvent(userId, eventId);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error while recording like for event {}: {}", eventId, e.getStatus());
            throw new ValidationException("Ошибка при записи лайка мероприятия");
        }
    }

    private boolean checkUserRegistration(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("События с id=" + eventId + " не существует"));
        return event.getConfirmedRequests() > 0;
    }
}