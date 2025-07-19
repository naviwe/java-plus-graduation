package ru.practicum;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class StatsRestClientImpl implements StatsRestClient {
    RestClient restClient;
    String url;
    private final StatsFeignClient statsFeignClient;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void addHit(EndpointHitDto hitDto) {
        try {
            statsFeignClient.hit(hitDto);
        } catch (Exception e) {
            log.info("Ошибка при обращении к эндпоинту /hit {}", e.getMessage(), e);
        }
    }

    public List<StatsDto> stats(String start, String end, List<String> uris, Boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).path("/stats")
                .queryParam("start", start)
                .queryParam("end", end);

        Optional.ofNullable(uris).ifPresent(id -> builder.queryParam("uris", uris.toArray()));
        Optional.ofNullable(unique).ifPresent(n -> builder.queryParam("unique", unique));

        URI uri = builder.build().toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, response) -> {
                    throw new ClientException(response.getStatusCode().value(), response.getBody().toString());
                }))
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
