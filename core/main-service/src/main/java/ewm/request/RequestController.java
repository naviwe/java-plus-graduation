package ewm.request;

import ewm.request.dto.ParticipationRequestDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestController {
    final String cancelPath = "/{requestId}/cancel";
    final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> findRequestsByUserId(@PathVariable Long userId) {
        return requestService.findRequestsByUserId(userId);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto saveRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        return requestService.saveRequest(userId, eventId);
    }

    @PatchMapping(cancelPath)
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }
}
