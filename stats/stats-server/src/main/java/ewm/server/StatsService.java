package ewm.server;

import ewm.EndpointHitDto;
import ewm.StatsDto;

import java.util.List;

public interface StatsService {
    EndpointHitDto saveHit(EndpointHitDto endpointHitDto);

    List<StatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}