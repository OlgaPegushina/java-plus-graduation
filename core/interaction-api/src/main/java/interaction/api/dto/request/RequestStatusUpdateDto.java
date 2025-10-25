package interaction.api.dto.request;

import interaction.api.dto.event.EventFullDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestStatusUpdateDto {
    @NotNull
    @Valid
    EventRequestStatusUpdateRequestDto updateRequest;

    @NotNull
    @Valid
    EventFullDto event;
}
