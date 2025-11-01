package aggregator.service;

import aggregator.kafka.config.AggregatorProperties;
import aggregator.kafka.producer.SimilarityProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AggregatorService {
    private final Map<Long, Map<Long, Double>> eventUserWeights;
    private final Map<Long, Map<Long, Double>> scalarResultMatrix;
    private final SimilarityProducer producer;
    private final Map<ActionTypeAvro, Double> actionWeights;

    public AggregatorService(SimilarityProducer producer, AggregatorProperties properties) {
        this.producer = producer;
        this.actionWeights = properties.getWeights(); // Получаем веса из properties

        this.eventUserWeights = new HashMap<>();
        this.scalarResultMatrix = new HashMap<>();

        log.info("AggregationService инициализирован с весами: {}", this.actionWeights);
    }

    public void calculateSimilarity(UserActionAvro request) {
        double newWeight = getWeight(request.getActionType());

        List<EventSimilarityAvro> similarities = updateEventWeight(
                request.getEventId(),
                request.getUserId(),
                newWeight
        );

        producer.sendSimilarityScores(similarities.stream()
                .sorted(Comparator.comparingLong(EventSimilarityAvro::getEventA)
                        .thenComparingLong(EventSimilarityAvro::getEventB))
                .collect(Collectors.toList()));
    }

/*    private double getWeight(ActionTypeAvro request) {
        switch (request) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 0.8;
            case LIKE:
                return 1.0;
            default:
                return 0.0;
        }
    }*/
    private double getWeight(ActionTypeAvro actionType) {
        return this.actionWeights.getOrDefault(actionType, 0.0);
    }

    private List<EventSimilarityAvro> updateEventWeight(Long eventId, Long userId, Double newWeight) {
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double currentWeight = userWeights.get(userId);

        if (currentWeight == null || currentWeight < newWeight) {
            List<EventSimilarityAvro> updatedSimilarities = recalculateSimilarities(
                    eventId,
                    userId,
                    newWeight,
                    currentWeight
            );
            userWeights.put(userId, newWeight);
            return updatedSimilarities;
        }
        return Collections.emptyList();
    }

    private List<EventSimilarityAvro> recalculateSimilarities(Long eventId, Long userId,
                                                              Double newWeight, Double oldWeight) {
        Map<Long, Double> selfDotProducts = scalarResultMatrix.computeIfAbsent(eventId, k -> new HashMap<>());
        double currentSelfProduct = selfDotProducts.getOrDefault(eventId, 0.0);
        double weightDelta = (oldWeight == null) ? newWeight : newWeight - oldWeight;

        selfDotProducts.put(eventId, currentSelfProduct + weightDelta);

        return updateCrossDotProducts(eventId, userId, newWeight, oldWeight);
    }

    private List<EventSimilarityAvro> updateCrossDotProducts(Long updatedEventId, Long userId,
                                                             Double newWeight, Double oldWeight) {
        List<EventSimilarityAvro> updatedSimilarities = new ArrayList<>();

        for (Long otherEventId : eventUserWeights.keySet()) {
            if (updatedEventId.equals(otherEventId)) continue;

            long eventA, eventB;
            boolean isUpdatedFirst;
            if (updatedEventId < otherEventId) {
                eventA = updatedEventId;
                eventB = otherEventId;
                isUpdatedFirst = true;
            } else {
                eventA = otherEventId;
                eventB = updatedEventId;
                isUpdatedFirst = false;
            }

            Map<Long, Double> otherUserWeights = eventUserWeights.get(otherEventId);
            if (otherUserWeights != null) {
                Double otherWeight = otherUserWeights.get(userId);
                if (otherWeight != null) {
                    EventSimilarityAvro similarity = updateDotProductForPair(
                            eventA, eventB, newWeight, oldWeight, otherWeight, isUpdatedFirst
                    );
                    if (similarity != null) {
                        updatedSimilarities.add(similarity);
                    }
                }
            }
        }
        return updatedSimilarities;
    }

    private EventSimilarityAvro updateDotProductForPair(long eventA, long eventB,
                                                        Double newWeight, Double oldWeight,
                                                        Double otherWeight, boolean isUpdatedFirst) {
        Map<Long, Double> dotProducts = scalarResultMatrix.computeIfAbsent(eventA, k -> new HashMap<>());

        double currentDotProduct = dotProducts.getOrDefault(eventB, 0.0);
        double oldMinWeight = (oldWeight == null) ? 0.0 : Math.min(oldWeight, otherWeight);
        double weightForMin;
        if (isUpdatedFirst) {
            weightForMin = newWeight;
        } else {
            weightForMin = otherWeight;
        }
        double newMinWeight = Math.min(weightForMin, isUpdatedFirst ? otherWeight : newWeight);

        double dotProductDelta = newMinWeight - oldMinWeight;

        double updatedDotProduct = currentDotProduct + dotProductDelta;
        dotProducts.put(eventB, updatedDotProduct);

        return calculateSimilarity(eventA, eventB, updatedDotProduct);
    }

    private EventSimilarityAvro calculateSimilarity(long eventA, long eventB, double dotProduct) {
        Double normA = calculateNorm(eventA);
        Double normB = calculateNorm(eventB);

        if (normA == null || normB == null || normA == 0 || normB == 0) {
            return null;
        }

        double similarity = dotProduct / (normA * normB);
        return new EventSimilarityAvro(eventA, eventB, similarity, Instant.now());
    }

    private Double calculateNorm(Long eventId) {
        Map<Long, Double> selfDotProducts = scalarResultMatrix.get(eventId);
        if (selfDotProducts == null) return null;

        Double selfProduct = selfDotProducts.get(eventId);
        return (selfProduct != null) ? Math.sqrt(selfProduct) : null;
    }
}
