package ewm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ewm.EndpointHitDto;
import ewm.StatsDto;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsFeignClient {

    @PostMapping("/hit")
    ResponseEntity<Void> saveHit(@RequestBody EndpointHitDto endpointHitDto);

    @GetMapping("/stats")
    List<StatsDto> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false, defaultValue = "false") boolean unique);
}