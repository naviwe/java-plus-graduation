package ewm.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ewm.exception.NotFoundException;
import ewm.user.User;
import ewm.user.UserRepository;


@Slf4j
@Service
@RequiredArgsConstructor
public class CheckUserService {
    private final UserRepository userRepository;

    public User checkUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d was not found", userId)));
    }
}
