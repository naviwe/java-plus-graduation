package ru.practicum;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EndpointHitDto {
    Long id;

    @NotBlank(message = "Имя должно быть указано")
    String app;

    @NotBlank(message = "Uri должен быть указан")
    String uri;

    @NotBlank(message = "IP должен быть указан")
    String ip;

    @NotNull(message = "Время должно быть указано")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp;
}
