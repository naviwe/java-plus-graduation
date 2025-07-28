package ewm.request;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.request.dto.ParticipationRequestDto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requesterId")
    @Mapping(target = "created", source = "created")
    ParticipationRequestDto toDto(Request request);

    List<ParticipationRequestDto> toDtoList(List<Request> requests);
}