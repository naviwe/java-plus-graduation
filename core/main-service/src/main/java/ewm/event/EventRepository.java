package ewm.event;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Event findByInitiatorId(Long userId);


    @EntityGraph(attributePaths = {"initiator", "category"})
    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND e.eventDate > :rangeStart " +
            "AND e.eventDate < :rangeEnd ")
    Page<Event> findAllEventsByAdmin(@Param("users") List<Long> users, @Param("states") List<State> states,
                           @Param("categories") List<Long> categories,
                           @Param("rangeStart") LocalDateTime rangeStart,
                           @Param("rangeEnd") LocalDateTime end,
                           Pageable pageable);


    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT e FROM Event e " +
            "WHERE ((:text IS NULL OR " +
            "       LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "       LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))) " +
            "AND (e.paid = :paid OR :paid IS NULL) " +
            "AND (e.category.id IN :categories OR :categories IS NULL) " +
            "AND (COALESCE(:onlyAvailable, false) = false OR e.participantLimit > e.confirmedRequests) " +
            "AND (e.eventDate > :rangeStart) " +
            "AND (e.eventDate < :rangeEnd) " +
            "AND (e.state = :state OR :state IS NULL)")
    Page<Event> findEvents(@Param("text") String text, @Param("paid") Boolean paid,
                           @Param("rangeStart") LocalDateTime rangeStart,
                           @Param("rangeEnd") LocalDateTime rangeEnd,
                           @Param("categories") List<Long> categories, Boolean onlyAvailable,
                           @Param("state") State state,
                           Pageable pageable);

    Optional<Event> findByIdAndState(Long id,State state);

    Boolean existsByCategoryId(Long catId);

}
