package ru.practicum.server;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of =  { "id" })
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "endpoint_hit", schema = "public")
@Getter
@Setter
@ToString
public class EndpointHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 20)
    String app;

    @Column(nullable = false, length = 255)
    String uri;

    @Column(nullable = false, length = 45)
    String ip;

    @Column(nullable = false)
    LocalDateTime timestamp;
}
