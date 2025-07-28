package ewm.user;

import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserDto userDto);

    UserDto toDto(User user);

    UserShortDto toShortDto(User user);
}
