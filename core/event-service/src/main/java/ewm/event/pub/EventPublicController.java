package ewm.event.pub;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.exception.ErrorResponse;
import ewm.interaction.exception.NotFoundException;
import ewm.interaction.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping(path = "/events")
@RequiredArgsConstructor
public class EventPublicController {

    EventPublicService eventPublicService;

    @GetMapping
    public ResponseEntity<?> getEvents(@RequestParam(required = false) String text,
                                       @RequestParam(required = false) List<Long> categories,
                                       @RequestParam(required = false) Boolean paid,
                                       @RequestParam(defaultValue = "") String rangeStart,
                                       @RequestParam(defaultValue = "") String rangeEnd,
                                       @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                       @RequestParam(defaultValue = "EVENT_DATE") String sort,
                                       @RequestParam(defaultValue = "0") Integer from,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       HttpServletRequest request) {
        try {
            List<EventShortDto> events = eventPublicService.getEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);
            return ResponseEntity.ok(events);
        } catch (ValidationException e) {
            log.error("Validation error in getEvents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Bad Request", "Invalid request parameters", e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error in getEvents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Unexpected error occurred", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id, HttpServletRequest request) {
        try {
            EventFullDto event = eventPublicService.getEventById(id, request);
            return ResponseEntity.ok(event);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Событие не найдено или не опубликовано",
                            "Event with id=" + id, e.getMessage()));
        }
    }
}
