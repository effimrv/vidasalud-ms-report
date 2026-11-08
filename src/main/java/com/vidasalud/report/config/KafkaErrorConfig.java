package com.vidasalud.report.config;

import com.vidasalud.report.eventos.MensajeInvalidoException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.lang.NonNull;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;
import java.util.Objects;

/**
 * Manejo de mensajes no procesables: se reintenta el error transitorio; si se agotan los reintentos,
 * o si el mensaje es inválido, se publica en el tópico DLT ({@code appointments.events.DLT}) y
 * queda registrado en el log con el motivo. El consumidor sigue avanzando.
 */
@Configuration
public class KafkaErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfig.class);

    /** Excepciones que no mejoran con reintentos: van directo al DLT. */
    private static final @NonNull Map<Class<? extends Throwable>, Boolean> NO_REINTENTABLES = Objects.requireNonNull(Map.of(
            MensajeInvalidoException.class, false,
            DeserializationException.class, false,
            SerializationException.class, false));

    @Bean
    public NewTopic topicoDeadLetter(
            @Value("${vidasalud.kafka.topicos.citas.nombre}") String topico,
            @Value("${vidasalud.kafka.topicos.citas.sufijo-dlt}") String sufijo,
            @Value("${vidasalud.kafka.topicos.citas.particiones}") int particiones,
            @Value("${vidasalud.kafka.topicos.citas.replicas}") short replicas) {
        return TopicBuilder.name(topico + sufijo).partitions(particiones).replicas(replicas).build();
    }

    @Bean
    public CommonErrorHandler errorHandler(
            KafkaTemplate<?, ?> kafkaTemplate,
            @Value("${vidasalud.kafka.topicos.citas.sufijo-dlt}") String sufijo,
            @Value("${vidasalud.kafka.reintentos.intervalo-ms}") long intervaloMs,
            @Value("${vidasalud.kafka.reintentos.maximo}") long maximo) {

        DeadLetterPublishingRecoverer aDlt = new DeadLetterPublishingRecoverer(Objects.requireNonNull(kafkaTemplate),
                (registro, error) -> new TopicPartition(registro.topic() + sufijo, -1));

        DefaultErrorHandler manejador = new DefaultErrorHandler((registro, error) -> {
            log.error("Mensaje enviado al DLT desde {} (partición {}, offset {}): {}", registro.topic(),
                    registro.partition(), registro.offset(), error.getMessage());
            aDlt.accept(registro, error);
        }, new FixedBackOff(intervaloMs, maximo));

        // Estos errores no mejoran con reintentos: van directo al DLT. traverseCauses=true porque el listener
        // lanza la excepción envuelta en ListenerExecutionFailedException y la causa es la que define el tipo.
        manejador.setClassifications(NO_REINTENTABLES, true);
        manejador.setRetryListeners((registro, error, intento) -> log.warn(
                "Reintento {} del mensaje en {} (partición {}, offset {}): {}", intento, registro.topic(),
                registro.partition(), registro.offset(), error.getMessage()));
        return manejador;
    }
}
