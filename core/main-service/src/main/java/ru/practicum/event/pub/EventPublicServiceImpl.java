package ru.practicum.event.pub;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.EndpointHitDto;
import ru.practicum.StatsDto;
import ru.practicum.StatsRestClient;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.State;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.exception.ValidationException;
import ru.practicum.utils.CheckEventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventPublicServiceImpl implements EventPublicService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    CheckEventService checkEventService;
    StatsRestClient statsClient;
    String app;

    static LocalDateTime minTime = LocalDateTime.of(1970, 1, 1, 0, 0);
    static LocalDateTime maxTime = LocalDateTime.of(3000, 1, 1, 0, 0);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public EventPublicServiceImpl(EventRepository eventRepository,
                                  EventMapper eventMapper,
                                  CheckEventService checkEventService,
                                  StatsRestClient statsClient,
                                  @Value("${my.app}") String app) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.checkEventService = checkEventService;
        this.statsClient = statsClient;
        this.app = app;
    }

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
                            return event1.getEventDate().compareTo(event2.getEventDate());
                        } else if (sort.equals("VIEWS")) {
                            return Long.compare(event1.getViews(), event2.getViews());
                        }
                        return 0;
                    })
                    .map(eventMapper::toShortDto)
                    .collect(Collectors.toList());
        }

        hitStats(request);

        List<String> uris = dtos.stream()
                .map(dto -> request.getRequestURI() + "/" + dto.getId())
                .collect(Collectors.toList());

        List<StatsDto> stats = getStats(request.getRequestURI(), uris);

        dtos.forEach(dto -> stats.stream()
                .filter(stat -> stat.getUri().equals(request.getRequestURI() + "/" + dto.getId()))
                .findFirst()
                .ifPresent(stat -> dto.setViews(stat.getHits())));

        if (dtos.isEmpty()) {
            throw new ValidationException("Нет подходящих событий");
        }
        return dtos;
    }

    @Override
    public EventFullDto getEventById(Long id, HttpServletRequest request) {
        Event event = checkEventService.checkPublishedEvent(id);
        EventFullDto eventFullDto = eventMapper.toFullDto(event);
        hitStats(request);

        List<StatsDto> stats = getStats(request.getRequestURI(), Collections.singletonList(request.getRequestURI()));
        if (!stats.isEmpty()) {
            eventFullDto.setViews(stats.getFirst().getHits());
        }
        return eventFullDto;
    }

    private void hitStats(HttpServletRequest request) {
        try {
            statsClient.addHit(EndpointHitDto.builder()
                    .app(app)
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage());
        }
    }

    private List<StatsDto> getStats(String requestUri, List<String> uris) {
        try {
            return statsClient.stats(
                    minTime.format(formatter),
                    maxTime.format(formatter),
                    uris,
                    true
            );
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для URI {}: {}", requestUri, e.getMessage());
            return Collections.emptyList();
        }
    }
}