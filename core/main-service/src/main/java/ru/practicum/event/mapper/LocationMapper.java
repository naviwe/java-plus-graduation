package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.event.Location;
import ru.practicum.event.dto.LocationDto;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    @Mapping(target = "lat", source = "lat")
    @Mapping(target = "lon", source = "lon")
    LocationDto toDto(Location location);
}
