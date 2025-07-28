package ewm.user;

import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;

import java.util.List;
import java.util.Map;

public interface UserService {

    UserDto createUser(UserDto userDto);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);

    Map<Long, UserShortDto> getMapUsers(List<Long> ids);
}
