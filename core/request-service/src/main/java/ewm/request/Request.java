package ewm.request;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ewm.interaction.dto.request.RequestStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "requests", schema = "public")
@EqualsAndHashCode(of = { "eventId", "requesterId" })
@Getter
@Setter
@ToString
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    LocalDateTime created;

    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    Long eventId;

    @JoinColumn(name = "requester_id", nullable = false)
    @ToString.Exclude
    Long requesterId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    RequestStatus status;
}