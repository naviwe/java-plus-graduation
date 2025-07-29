package ewm.event;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.NewEventDto;
import ewm.interaction.dto.event.UpdateEventRequest;
import ewm.interaction.dto.request.EventRequestStatusUpdateRequest;
import ewm.interaction.dto.request.EventRequestStatusUpdateResult;
import ewm.interaction.dto.request.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto saveEvent(NewEventDto newEventDto, Long userId);

    List<EventShortDto> findEventsByInitiatorId(Long userId, Integer from, Integer size);

    EventFullDto findById(Long userId, Long eventId);

    EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long userId, Long eventId);
}

