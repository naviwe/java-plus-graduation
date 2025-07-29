package ewm.interaction.client;

import ewm.interaction.dto.event.EventFullDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "event-service", path = "/events/feign")
public interface EventFeignClient {

    @GetMapping("/events/{eventId}")
    EventFullDto findById(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/{id}")
    EventFullDto getEventByIdInternal(@PathVariable Long id);

    @PutMapping
    void changeEventFields(@RequestBody EventFullDto eventFullDto);
}