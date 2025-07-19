package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsFeignClient {

    @PostMapping("/hit")
    void saveHit(@RequestBody EndpointHitDto endpointHitDto);

    @GetMapping("/stats")
    List<StatsDto> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique
    );
}