package ru.practicum;

import java.util.List;

public interface StatsRestClient {
    void addHit(EndpointHitDto hitDto);

    List<StatsDto> stats(String start, String end, List<String> uris, Boolean unique);
}