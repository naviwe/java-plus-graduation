package ewm.interaction.feign;

import ewm.interaction.dto.request.ParticipationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "request-service", url = "/request")
public interface RequestFeignClient {
    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> findRequestsByUserId(@RequestParam Long userId);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> findRequestsByEventId(@RequestParam Long userId, @RequestParam Long eventId);
}