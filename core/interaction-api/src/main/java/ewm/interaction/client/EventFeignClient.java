package ewm.interaction.client;

import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.UpdateEventRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "event-service", path = "/events/feign")
public interface EventFeignClient {

    @GetMapping("/events/{eventId}")
    EventFullDto findById(@PathVariable("eventId") Long eventId);

    @PatchMapping("/admin/events/{eventId}")
    EventFullDto updateEvent(@PathVariable("eventId") Long eventId, @RequestBody UpdateEventRequest updateEventRequest);
}