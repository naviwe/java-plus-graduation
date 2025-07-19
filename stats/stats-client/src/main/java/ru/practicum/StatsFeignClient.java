package ru.practicum;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsFeignClient {
    @PostMapping("/hit")
    void hit(@Valid @RequestBody EndpointHitDto endpointHitDto);

    @GetMapping("/stats")
    List<StatsDto> stats(@RequestParam("start") String start,
                             @RequestParam("end") String end,
                             @RequestParam("uris") List<String> uris,
                             @RequestParam("unique") boolean unique);

}