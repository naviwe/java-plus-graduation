package ewm.event.mapper;

import ewm.event.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.event.dto.LocationDto;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    @Mapping(target = "lat", source = "lat")
    @Mapping(target = "lon", source = "lon")
    LocationDto toDto(Location location);
}
