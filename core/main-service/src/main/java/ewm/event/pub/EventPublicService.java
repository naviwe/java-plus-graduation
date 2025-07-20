package ewm.event.pub;

import jakarta.servlet.http.HttpServletRequest;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;

import java.util.List;

public interface EventPublicService {

    List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid, String rangeStart,
                                  String rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                  Integer size,HttpServletRequest request);

    EventFullDto getEventById(Long id, HttpServletRequest request);
}
