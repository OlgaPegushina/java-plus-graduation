package ru.practicum.kafka.serializer;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Универсальный десериализатор
 * Класс работает с целевым типом Class<T>, чтобы интегрировать его в конфигурацию ConsumerFactory.
 * @param <T> Тип Avro-объекта, который должен быть десериализован.
 */
public class GeneralAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    /**
     * Целевой класс, в который десериализуем данные.
     */
    protected final Class<T> targetType;

    /**
     * Конструктор по умолчанию. Необходим для соответствия контракту Kafka Deserializer,
     */
    public GeneralAvroDeserializer() {
        this.targetType = null;
    }

    /**
     * Конструктор, который используется в конфигурации Spring Kafka.
     * Принимает класс целевого типа и сохраняет его для последующего использования.
     * @param targetType Класс Avro-сообщения.
     */
    public GeneralAvroDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // -- Spring Kafka передаст нужный класс через конструктор.
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        if (targetType == null) {
            throw new IllegalStateException("Целевой тип targetType не сконфигурирован для этого десериализатора.");
        }

        try {
            // -- Создаем SpecificDatumReader, передаем ему наш целевой класс, и он сам извлечет из него нужную схему.
            DatumReader<T> datumReader = new SpecificDatumReader<>(targetType);

            // -- Создаем бинарный декодер, который будет читать массив байтов.
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);

            // -- Читаем данные с помощью ридера и декодера, получая на выходе готовый Java-объект.
            return datumReader.read(null, decoder);

        } catch (IOException e) {
            // Если что-то пошло не так в процессе чтения, выбрасываем стандартное исключение Kafka.
            throw new SerializationException(
                    String.format("Ошибка десериализации данных из топика [%s]. Data: %s",
                            topic, Arrays.toString(data)), e);
        }
    }

    @Override
    public void close() {
        // -- Никаких ресурсов для освобождения нет.
    }
}
