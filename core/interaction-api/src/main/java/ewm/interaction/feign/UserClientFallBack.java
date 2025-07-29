package ewm.interaction.feign;

import ewm.interaction.dto.user.UserDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserClientFallBack implements UserFeignClient {
    @Override
    public UserDto getUser(Long userId) {
        return UserDto.builder().build();
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        return List.of();
    }
}