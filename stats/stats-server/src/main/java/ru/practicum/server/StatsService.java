package ru.practicum.server;

import ru.practicum.EndpointHitDto;
import ru.practicum.StatsDto;

import java.util.List;

public interface StatsService {
    EndpointHitDto saveHit(EndpointHitDto endpointHitDto);

    List<StatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}
