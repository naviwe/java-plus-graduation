package ewm.interaction.client;

import ewm.interaction.dto.request.ParticipationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "request-service", path = "/users/{userId}/requests")
public interface RequestFeignClient {

    @GetMapping
    List<ParticipationRequestDto> findRequestsByUserId(@PathVariable("userId") Long userId);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ParticipationRequestDto saveRequest(@PathVariable("userId") Long userId, @RequestParam("eventId") Long eventId);

    @PatchMapping("/{requestId}/cancel")
    ParticipationRequestDto cancelRequest(@PathVariable("userId") Long userId, @PathVariable("requestId") Long requestId);

}