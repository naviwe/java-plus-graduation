package ewm.comment;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = { "text", "eventId", "authorId" })
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "comments", schema = "public")
@Getter
@Setter
@ToString
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 2000)
    String text;

    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    Long eventId;

    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    Long authorId;

    @Column(nullable = false)
    LocalDateTime created;

    @Column(nullable = false)
    LocalDateTime updated;

    @Column(name = "updated_by", nullable = false)
    String updatedBy;
}