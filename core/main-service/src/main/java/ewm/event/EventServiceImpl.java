package ewm.event;

import ewm.request.Request;
import ewm.request.RequestMapper;
import ewm.request.RequestRepository;
import ewm.request.RequestStatus;
import ewm.request.dto.EventRequestStatusUpdateRequest;
import ewm.request.dto.EventRequestStatusUpdateResult;
import ewm.request.dto.ParticipationRequestDto;
import ewm.utils.CheckCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.NewEventDto;
import ewm.event.dto.UpdateEventRequest;
import ewm.category.model.Category;
import ewm.event.mapper.EventMapper;
import ewm.exception.ConflictException;
import ewm.utils.CheckEventService;
import ewm.utils.CheckUserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ewm.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {
    EventRepository eventRepository;
    CheckUserService checkUserService;
    CheckEventService checkEventService;
    EventMapper eventMapper;
    RequestMapper requestMapper;
    CheckCategoryService checkCategoryService;
    RequestRepository requestRepository;
    LocationRepository locationRepository;

    @Override
    public List<EventShortDto> findEventsByInitiatorId(Long userId, Integer from, Integer size) {
        checkUserService.checkUser(userId);
        Pageable pageRequest = PageRequest.of(from / size, size);
        Page<Event> eventPage = eventRepository.findByInitiatorId(userId, pageRequest);
        return logAndReturn(eventPage.getContent()
                        .stream()
                        .map(eventMapper::toShortDto)
                        .toList(),
                events -> log.info("Found {} events for user with id={}",
                        events.size(), userId)
        );
    }

    @Override
    public EventFullDto findById(Long userId, Long eventId) {
        checkUserService.checkUser(userId);

        Event event = checkEventService.checkEvent(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format(
                    "User with id=%d isn't an initiator for event with id=%d",
                    userId, eventId));
        }

        return logAndReturn(eventMapper.toFullDto(event),
                e -> log.info("Found event with id={}", eventId));
    }

    @Override
    @Transactional
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        Event event = eventMapper.toEvent(newEventDto, userId);
        Location location = event.getLocation();
        if (location != null) {
            location = locationRepository.save(location);
            event.setLocation(location);
        }
        event.setInitiatorId(checkUserService.checkUser(userId));
        event.setCategory(checkCategoryService.checkCategory(newEventDto.getCategory()));
        event.setCreatedOn(LocalDateTime.now());
        event.setState(State.PENDING);
        event.setConfirmedRequests(0L);
        event.setViews(0L);
        return logAndReturn(eventMapper.toFullDto(eventRepository.save(event)),
                dto -> log.info("Event created successfully: {}", dto)
        );
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long userId, Long eventId) {
        checkUserService.checkUser(userId);
        Event event = checkEventService.checkEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d isnt a initiator for event with id=%d",
                    userId, eventId));
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
                    throw new IllegalArgumentException("Неподдерживаемое действие: "
                            + updateEventRequest.getStateAction());
            }
        }
        return logAndReturn(eventMapper.toFullDto(eventRepository.save(event)),
                dto -> log.info("Event updated successfully: {}", dto)
        );
    }

    @Override
    public List<ParticipationRequestDto> findRequestsByEventId(Long userId, Long eventId) {
        checkUserService.checkUser(userId);
        Event event = checkEventService.checkEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format(
                    "User with id=%d isn't an initiator for event with id=%d", userId, eventId));
        }
        List<Request> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(EventRequestStatusUpdateRequest requestDto, Long userId,
                                                              Long eventId) {
        checkUserService.checkUser(userId);
        Event event = checkEventService.checkEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d isn't an initiator for event with id=%d", userId,
                    eventId));
        }

        if ((event.getParticipantLimit() != 0) && (event.getParticipantLimit()
                .equals(event.getConfirmedRequests().intValue()))) {
            throw new ConflictException("There is no more space");
        }
        List<Request> requests = requestRepository.findAllByIdInAndStatus(requestDto.getRequestIds(),
                RequestStatus.PENDING);
        if (requests.size() != requestDto.getRequestIds().size()) {
            throw new ConflictException("Some requests are not in PENDING status or do not exist");
        }
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
        if (requestDto.getStatus().equals(RequestStatus.CONFIRMED)) {
            confirmedRequests = processRequests(requests, requestDto.getStatus());
            result.setConfirmedRequests(confirmedRequests);
            event.setConfirmedRequests(event.getConfirmedRequests() + confirmedRequests.size());
        } else if (requestDto.getStatus().equals(RequestStatus.REJECTED)) {
            confirmedRequests = null;
            rejectedRequests = processRequests(requests, requestDto.getStatus());
            result.setRejectedRequests(rejectedRequests);
        }
        eventRepository.save(event);
        return result;
    }

    private List<ParticipationRequestDto> processRequests(List<Request> requests, RequestStatus status) {
        return requests.stream()
                .map(request -> {
                    request.setStatus(status);
                    requestRepository.save(request);
                    ParticipationRequestDto dto = requestMapper.toDto(request);
                    dto.setStatus(status);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
