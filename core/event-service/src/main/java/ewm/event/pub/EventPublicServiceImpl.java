package ewm.event.pub;

import ewm.event.Event;
import ewm.event.EventRepository;
import ewm.event.mapper.EventMapper;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.State;
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
import ewm.EndpointHitDto;
import ewm.StatsDto;
import ewm.StatsClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventPublicServiceImpl implements EventPublicService {

    private EventRepository eventRepository;
    private EventMapper eventMapper;
    private EventValidationService checkEventService;


    private String app;
    private StatsClient client;

    public EventPublicServiceImpl(EventRepository eventRepository,
                                  EventMapper eventMapper,
                                  EventValidationService checkEventService,
                                  @Value("${my.app}") String app,
                                  StatsClient client) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.checkEventService = checkEventService;
        this.app = app;
        this.client = client;
    }

    static LocalDateTime minTime = LocalDateTime.of(1970, 1, 1, 0, 0);
    static LocalDateTime maxTime = LocalDateTime.of(3000, 1, 1, 0, 0);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                         String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                         String sort, Integer from, Integer size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(from / size, size);

        if (from < 0 || size <= 0) {
            throw new ValidationException("Invalid pagination parameters");
        }

        LocalDateTime start = rangeStart != null && !rangeStart.isEmpty() ?
                LocalDateTime.parse(rangeStart, formatter) : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null && !rangeEnd.isEmpty() ?
                LocalDateTime.parse(rangeEnd, formatter) : maxTime;
        text = text != null ? text.toLowerCase() : "";

        Page<Event> events = eventRepository.findEvents(text, paid, start, end, categories, onlyAvailable,
                State.PUBLISHED, pageable);
        List<EventShortDto> dtos = events.getContent().stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());

        if (!dtos.isEmpty() && sort != null) {
            dtos = dtos.stream()
                    .sorted((dto1, dto2) -> {
                        if (sort.equals("EVENT_DATE")) {
                            return dto1.getEventDate().compareTo(dto2.getEventDate());
                        } else if (sort.equals("VIEWS")) {
                            return Long.compare(dto2.getViews(), dto1.getViews());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
        }

        hitStats(request);

        if (!dtos.isEmpty()) {
            List<String> uris = dtos.stream()
                    .map(dto -> "/events/" + dto.getId())
                    .collect(Collectors.toList());

            try {
                List<StatsDto> stats = client.getStats(minTime.format(formatter), maxTime.format(formatter), uris, true);
                for (EventShortDto dto : dtos) {
                    stats.stream()
                            .filter(stat -> stat.getUri().equals("/events/" + dto.getId()))
                            .findFirst()
                            .ifPresent(stat -> dto.setViews(stat.getHits()));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch stats: {}", e.getMessage());
            }
        }

        return dtos;
    }

    @Override
    public EventFullDto getEventById(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, State.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found or not published"));

        hitStats(request);

        List<StatsDto> stats = getStats(request);
        long views = stats.isEmpty() ? 0 : stats.getFirst().getHits();

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setViews(views);

        return dto;
    }

    private void hitStats(HttpServletRequest request) {
        client.saveHit(EndpointHitDto.builder().app(app).uri(request.getRequestURI()).ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now()).build());
    }

    private List<StatsDto> getStats(HttpServletRequest request) {
        return client.getStats(minTime.format(formatter),
                maxTime.format(formatter), List.of(request.getRequestURI()), true);
    }
}
