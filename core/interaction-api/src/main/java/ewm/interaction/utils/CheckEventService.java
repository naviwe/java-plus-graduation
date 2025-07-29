package ewm.interaction.utils;

import ewm.interaction.client.EventFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckEventService {
    private final EventFeignClient eventClient;

    public EventFullDto checkEvent(Long eventId) throws NotFoundException {
        try {
            EventFullDto event = eventClient.findById(eventId);
            return event;
        } catch (Exception e) {
            log.error("Event with id={} was not found", eventId, e);
            throw new NotFoundException(String.format("Event with id=%d was not found", eventId));
        }
    }
}