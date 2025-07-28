package ewm.event.pub;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(defaultValue = "") String rangeStart,
                                         @RequestParam(defaultValue = "") String rangeEnd,
                                         @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                         @RequestParam(defaultValue = "EVENT_DATE") String sort,
                                         @RequestParam(defaultValue = "0") Integer from,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         HttpServletRequest request) {

        return eventPublicService.getEvents(text,categories,paid,rangeStart,rangeEnd,onlyAvailable,sort,from,size,
                request);

    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventById(@PathVariable Long id, HttpServletRequest request) {
        try {
            EventFullDto event = eventPublicService.getEventById(id, request);
            return ResponseEntity.ok(event);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


}
