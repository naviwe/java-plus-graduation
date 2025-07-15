package ru.practicum.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "locations", schema = "public")
@EqualsAndHashCode(of = { "lat", "lon" })
@Getter
@Setter
@ToString
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Double lat;

    @Column(nullable = false)
    Double lon;
}
