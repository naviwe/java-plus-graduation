package ewm.event.admin;

import ewm.event.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.category.model.Category;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventRequest;
import ewm.event.mapper.EventMapper;
import ewm.exception.ConflictException;
import ewm.exception.ForbiddenException;
import ewm.utils.CheckCategoryService;
import ewm.utils.CheckEventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ewm.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventAdminServiceImpl implements EventAdminService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    LocationRepository locationRepository;
    CheckEventService checkEventService;
    CheckCategoryService checkCategoryService;

    static LocalDateTime minTime = LocalDateTime.of(1970, 1, 1, 0, 0);
    static LocalDateTime maxTime = LocalDateTime.of(3000, 1, 1, 0, 0);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventFullDto> getEvents(List<Long> users, List<String> statesStr,
                                        List<Long> categories, String rangeStart,
                                        String rangeEnd, Integer from, Integer size) {

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) :
                minTime;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) :
                maxTime;

        List<State> states = statesStr != null ? statesStr.stream().map(State::valueOf).toList() : null;

        Pageable pageable = PageRequest.of(from / size, size);
        return eventRepository.findAllEventsByAdmin(users, states,
                categories, start,
                end, pageable).stream().map(eventMapper::toFullDto).toList();

    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long eventId) {
        if (updateEventRequest == null) {
            throw new ForbiddenException("Cannot publish the event: request body is missing.");
        }
        Event event = checkEventService.checkEvent(eventId);

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
            Location location = locationRepository.findByLatAndLon(lat,
                            updateEventRequest.getLocation().getLon())
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

        return logAndReturn(eventMapper.toFullDto(eventRepository.save(event)),
                dto -> log.info("Event updated successfully: {}", dto)
        );
    }

}
