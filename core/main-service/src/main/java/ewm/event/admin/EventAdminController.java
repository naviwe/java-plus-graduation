package ewm.event.admin;


import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
public class EventAdminController {

    EventAdminService eventAdminService;

    @GetMapping
    public List<EventFullDto> getEvents(@RequestParam(required = false) List<Long> users,
                                        @RequestParam(required = false) List<String> states,
                                        @RequestParam(required = false) List<Long> categories,
                                        @RequestParam(required = false) String rangeStart,
                                        @RequestParam(required = false) String rangeEnd,
                                        @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                        @Positive @RequestParam(defaultValue = "10") Integer size) {

        return eventAdminService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @Transactional
    @PatchMapping("/{eventId}")
    public EventFullDto publishEvent(@RequestBody(required = false) @Valid UpdateEventRequest updateEventRequest,
                                      @PathVariable Long eventId) {

        return eventAdminService.updateEvent(updateEventRequest,eventId);

    }
}
