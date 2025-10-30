package aggregator.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Component
@Slf4j
public class SimilarityProducer {
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final String topicName;

    public SimilarityProducer(KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate,
                              @Value("${spring.kafka.producer.topic.events-similarity}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void sendSimilarityScore(long eventId1, long eventId2, double score, Instant timestamp) {
        // -- Упорядочивание Id
        long eventA = Math.min(eventId1, eventId2);
        long eventB = Math.max(eventId1, eventId2);

        EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();

        log.debug("Подготовка к отправке оценки сходства в топик '{}': {}", topicName, message);

        kafkaTemplate.send(topicName, String.valueOf(eventA), message)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        log.info("Оценка сходства успешно отправлена: message={}, topic={}, partition={}, offset={}",
                                message,
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке оценки сходства в топик '{}': message={}",
                                topicName, message, exception);
                    }
                });
        log.debug("Отправлена оценка сходства: {}", message);
    }
}
