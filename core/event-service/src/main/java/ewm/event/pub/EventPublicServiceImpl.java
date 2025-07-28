package ewm.event.pub;

import ewm.event.Event;
import ewm.event.EventRepository;
import ewm.event.mapper.EventMapper;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.State;
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

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : minTime;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : maxTime;
        text = text != null ? text : "";
        Page<Event> events = eventRepository.findEvents(text, paid, start, end, categories, onlyAvailable,
                State.PUBLISHED, pageable);
        List<EventShortDto> dtos = events.map(eventMapper::toShortDto).toList();

        if (sort != null) {
            dtos = events.stream()
                    .sorted((event1, event2) -> {
                        if (sort.equals("EVENT_DATE")) {
                            if (event1.getEventDate().isBefore(event2.getEventDate()))
                                return -1;
                            else
                                return 1;
                        } else if (sort.equals("VIEWS")) {
                            return (int) (event2.getViews() - event1.getViews());
                        }
                        return 1;
                    })
                    .map(eventMapper::toShortDto)
                    .toList();
        }

        hitStats(request);

        List<String> uris = dtos.stream()
                .map(dto -> request.getRequestURI() + "/" + dto.getId())
                .collect(Collectors.toList());

        List<StatsDto> stats = client.getStats(minTime.format(formatter), maxTime.format(formatter), uris, true);

        for (EventShortDto dto : dtos) {
            stats.stream()
                    .filter(stat -> stat.getUri().equals(request.getRequestURI() + "/" + dto.getId()))
                    .findFirst()
                    .ifPresent(stat -> dto.setViews(stat.getHits()));
        }

        if (dtos.isEmpty()) {
            throw new ValidationException("Нет подходящих событий");
        } else {
            return dtos;
        }
    }

    @Override
    public EventFullDto getEventById(Long id, HttpServletRequest request) {
        EventFullDto eventFullDto = eventMapper.toFullDto(checkEventService.checkPublishedEvent(id));
        hitStats(request);
        eventFullDto.setViews(getStats(request).getFirst().getHits());
        return eventFullDto;
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
