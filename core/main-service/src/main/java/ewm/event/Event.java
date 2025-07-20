package ewm.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ewm.category.model.Category;
import ewm.user.User;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "events", schema = "public")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@ToString
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 120)
    String title;

    @Column(nullable = false, length = 2000)
    String annotation;

    @Column(nullable = false, length = 7000)
    String description;

    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;

    @Column(nullable = false)
    Boolean paid;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    Integer participantLimit;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    Boolean requestModeration;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    Long confirmedRequests;

    @Column(nullable = false)
    LocalDateTime createdOn;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    State state;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    Long views;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    @ToString.Exclude
    User initiator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @ToString.Exclude
    Location location;
}