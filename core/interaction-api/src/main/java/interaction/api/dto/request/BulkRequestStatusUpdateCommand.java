package interaction.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkRequestStatusUpdateCommand {
    @NotNull
    @Positive
    private Long initiatorId;

    @NotNull @Positive
    private Long eventId;

    @Valid
    @NotNull
    private RequestStatusUpdateDto updateDto;
}