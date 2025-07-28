package ewm.utils;

import ewm.client.UserFeignClient;
import ewm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckUserService {

    private final UserFeignClient userFeignClient;

    public long checkUser(Long userId) {
        try {
            userFeignClient.getUsers(List.of(userId), 0, 1);
        } catch (Exception e) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        return userId;
    }
}