package ewm.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ewm.interaction.dto.request.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequesterId(Long userId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);
}
