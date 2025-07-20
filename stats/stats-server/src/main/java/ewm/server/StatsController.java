package ewm.server;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ewm.EndpointHitDto;
import ewm.StatsDto;
import ewm.exception.InternalServerException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatsController {
    final String hitPath = "/hit";
    final String statsPath = "/stats";

    final StatsService statsService;

    @PostMapping(hitPath)
    public ResponseEntity<Void> saveHit(@Valid @RequestBody EndpointHitDto endpointHitDto) {
        EndpointHitDto savedEndpointHit = statsService.saveHit(endpointHitDto);
        if (savedEndpointHit.equals(endpointHitDto)) {
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            throw new InternalServerException("Данные не записались на сервере");
        }
    }

    @GetMapping(statsPath)
    public List<StatsDto> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false, defaultValue = "false") boolean unique) {
        return statsService.getStats(start, end, uris, unique);
    }
}
