package ewm.event.pub;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

public interface EventPublicService {

    List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid, String rangeStart,
                                  String rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                  Integer size, HttpServletRequest request);

    EventFullDto getEventById(Long id, HttpServletRequest request, @RequestHeader("X-EWM-USER-ID") Long userId);

    void changeEventFields(EventFullDto eventFullDto);

    EventFullDto getEventByIdInternal(Long id);

    List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId, Integer from, Integer size);

    void likeEvent(Long eventId, @RequestHeader("X-EWM-USER-ID") Long userId);
}