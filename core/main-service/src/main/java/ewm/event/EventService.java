package ewm.event;

import ewm.request.dto.EventRequestStatusUpdateRequest;
import ewm.request.dto.EventRequestStatusUpdateResult;
import ewm.request.dto.ParticipationRequestDto;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.NewEventDto;
import ewm.event.dto.UpdateEventRequest;

import java.util.List;

public interface EventService {
    EventFullDto saveEvent(NewEventDto newEventDto, Long userId);

    List<EventShortDto> findEventsByInitiatorId(Long userId, Integer from, Integer size);

    EventFullDto findById(Long userId, Long eventId);

    EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long userId, Long eventId);

    List<ParticipationRequestDto> findRequestsByEventId(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(EventRequestStatusUpdateRequest requestDto, Long userId,
                                                       Long eventId);
}

