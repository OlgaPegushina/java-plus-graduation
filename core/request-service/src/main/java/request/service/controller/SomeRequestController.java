package request.service.controller;

import interaction.api.dto.request.BulkRequestStatusUpdateCommand;
import interaction.api.dto.request.EventRequestStatusUpdateResultDto;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import request.service.service.RequestService;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SomeRequestController {
    RequestService requestService;

    @PatchMapping("/requests/status/bulk-update")
    public EventRequestStatusUpdateResultDto bulkUpdateStatus(
            @Valid @RequestBody BulkRequestStatusUpdateCommand command) {
        return requestService.updateParticipationRequestsStatus(
                command.getInitiatorId(),
                command.getEventId(),
                command.getUpdateDto()
        );
    }
}

