package aggregator.kafka.consumer;

import aggregator.service.SimilarityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserActionListener {
    SimilarityService similarityService;

    @KafkaListener(topics = "${spring.kafka.consumer.topic.user-actions}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserAction(UserActionAvro message) {

        log.info("Получен user action: {}", message);

        // -- вся логика реализована в сервисе
        similarityService.processUserAction(message);
    }
}
