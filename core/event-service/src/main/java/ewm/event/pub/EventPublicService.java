package ewm.event.pub;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface EventPublicService {

    List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid, String rangeStart,
                                  String rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                  Integer size, HttpServletRequest request);

    EventFullDto getEventById(Long id, HttpServletRequest request);

    void changeEventFields(EventFullDto eventFullDto);


    EventFullDto getEventByIdInternal(Long id);
}
