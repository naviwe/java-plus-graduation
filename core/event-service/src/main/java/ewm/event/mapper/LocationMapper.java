package ewm.event.mapper;

import ewm.event.Location;
import ewm.interaction.dto.event.LocationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    @Mapping(target = "lat", source = "lat")
    @Mapping(target = "lon", source = "lon")
    LocationDto toDto(Location location);
}
