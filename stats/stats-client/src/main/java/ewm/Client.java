package ewm;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ewm.EndpointHitDto;
import ewm.StatsDto;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Client {
    RestClient restClient;

    String url;

    public Client(String url) {
        this.restClient = RestClient.create();
        this.url = url;
    }

    public void hit(EndpointHitDto endpointHitDto) {
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(url).path("/hit").build();

        restClient.post()
                .uri(uriComponents.toUri())
                .body(endpointHitDto)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, response) -> {
                    throw new ClientException(response.getStatusCode().value(), response.getBody().toString());
                }));
    }

    public List<StatsDto> getStats(String start, String end) {
        return getStats(start, end, null, null);
    }

    public List<StatsDto> getStats(String start, String end, List<String> uris) {
        return getStats(start, end, uris, null);
    }

    public List<StatsDto> getStats(String start, String end, Boolean unique) {
        return getStats(start, end, null, unique);
    }

    public List<StatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
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