package ewm.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ewm.exception.NotFoundException;
import ewm.request.Request;
import ewm.request.RequestRepository;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CheckRequestService {
    RequestRepository requestRepository;

    public Request checkRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d was not found", requestId)));
    }
}
