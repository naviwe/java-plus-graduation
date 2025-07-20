package ewm.server;

import lombok.NoArgsConstructor;
import ewm.EndpointHitDto;


@NoArgsConstructor
public class StatsMapper {
    public static EndpointHit mapToEndpointHit(EndpointHitDto endpointHitDto) {
        EndpointHit endpoint = new EndpointHit();
        endpoint.setApp(endpointHitDto.getApp());
        endpoint.setUri(endpointHitDto.getUri());
        endpoint.setIp(endpointHitDto.getIp());
        endpoint.setTimestamp(endpointHitDto.getTimestamp());
        return endpoint;
    }

    public static EndpointHitDto mapToEndpointHitDto(EndpointHit endpointHit) {
        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp(endpointHit.getApp());
        dto.setUri(endpointHit.getUri());
        dto.setIp(endpointHit.getIp());
        dto.setTimestamp(endpointHit.getTimestamp());
        return dto;
    }
}