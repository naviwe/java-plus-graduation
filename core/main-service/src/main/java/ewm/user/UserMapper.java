package ewm.user;

import ewm.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.user.dto.UserShortDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserDto userDto);

    UserDto toDto(User user);

    UserShortDto toShortDto(User user);
}