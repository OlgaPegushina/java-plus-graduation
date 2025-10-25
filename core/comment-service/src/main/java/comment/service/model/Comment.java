package comment.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    @NotNull(message = "Id не может быть null")
    @Positive(message = "Id должен быть положительным")
    Long id;

    @Column(name = "event_id", nullable = false)
    @NotNull(message = "Event ID не может быть null")
    @Positive(message = "Event ID должен быть положительным")
    Long event;

    @Column(name = "author_id", nullable = false)
    @NotNull(message = "Author ID не может быть null")
    @Positive(message = "Author ID должен быть положительным")
    Long authorId;

    @Column(name = "message", length = 1000)
    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(max = 1000, message = "Сообщение не может превышать 1000 символов")
    String message;

    @Column(name = "created_on")
    LocalDateTime createdOn;

    @Column(name = "updated_on")
    LocalDateTime updatedOn;
}
