package ewm.event;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.dto.event.NewEventDto;
import ewm.interaction.dto.event.UpdateEventRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventPrivateController {
    final String eventIdPath = "/{eventId}";
    final EventService eventService;

    @GetMapping
    public List<EventShortDto> findEventsByInitiatorId(@PathVariable Long userId,
                                                       @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                                       @Positive @RequestParam(defaultValue = "10") Integer size) {
        return eventService.findEventsByInitiatorId(userId, from, size);
    }

    @GetMapping(eventIdPath)
    public EventFullDto findById(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.findById(userId, eventId);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto saveEvent(@RequestBody @Valid NewEventDto newEventDto,
                                  @PathVariable Long userId) {
        return eventService.saveEvent(newEventDto, userId);
    }

    @PatchMapping(eventIdPath)
    public EventFullDto updateEvent(@Valid @RequestBody UpdateEventRequest updateEventRequest,
                                    @PathVariable Long userId,
                                    @PathVariable Long eventId) {
        return eventService.updateEvent(updateEventRequest, userId, eventId);
    }
}

