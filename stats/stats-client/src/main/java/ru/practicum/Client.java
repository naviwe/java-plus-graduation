package ru.practicum;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Client {
    StatsFeignClient feignClient;

    public Client(StatsFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public void hit(EndpointHitDto endpointHitDto) {
        try {
            feignClient.saveHit(endpointHitDto);
        } catch (Exception e) {
            throw new ClientException(500, "Error calling stats service: " + e.getMessage());
        }
    }

    public List<StatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            return feignClient.getStats(start, end, uris, unique);
        } catch (Exception e) {
            throw new ClientException(500, "Error getting stats: " + e.getMessage());
        }
    }
}