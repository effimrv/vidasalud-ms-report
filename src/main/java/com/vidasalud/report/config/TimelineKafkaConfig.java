package com.vidasalud.report.config;

import com.vidasalud.report.eventos.EntradaAuditoria;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumo de audit.timeline con su propio tipo de mensaje ({@link EntradaAuditoria}). Se configura aparte
 * porque el consumidor de citas usa otro DTO: cada tópico declara el suyo y no depende del orden de llegada.
 */
@Configuration
public class TimelineKafkaConfig {

    private static final String PAQUETE_EVENTOS = "com.vidasalud.report.eventos";

    @Bean
    public ConsumerFactory<String, EntradaAuditoria> timelineConsumerFactory(KafkaProperties propiedades) {
        Map<String, Object> config = new HashMap<>(propiedades.buildConsumerProperties());
        // Los tipos los fija el código de este bean, no las propiedades globales (que apuntan a CitaEvento).
        config.remove("spring.json.value.default.type");
        config.remove("spring.deserializer.value.delegate.class");
        config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        config.remove(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        // Los ajustes JSON los fija el código (constructor y addTrustedPackages), no las propiedades.
        config.keySet().removeIf(clave -> clave.startsWith("spring.json"));

        JsonDeserializer<EntradaAuditoria> json = new JsonDeserializer<>(EntradaAuditoria.class, false);
        json.addTrustedPackages(PAQUETE_EVENTOS);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new ErrorHandlingDeserializer<>(json));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EntradaAuditoria> timelineContainerFactory(
            ConsumerFactory<String, EntradaAuditoria> timelineConsumerFactory,
            CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, EntradaAuditoria> fabrica = new ConcurrentKafkaListenerContainerFactory<>();
        fabrica.setConsumerFactory(timelineConsumerFactory);
        fabrica.setCommonErrorHandler(errorHandler);
        return fabrica;
    }

    /** Dead Letter Topic de audit.timeline: lo usa el manejo de errores de este consumidor. */
    @Bean
    public NewTopic topicoTimelineDeadLetter(
            @Value("${vidasalud.kafka.topicos.timeline.nombre}") String topico,
            @Value("${vidasalud.kafka.topicos.citas.sufijo-dlt}") String sufijo,
            @Value("${vidasalud.kafka.topicos.citas.particiones}") int particiones,
            @Value("${vidasalud.kafka.topicos.citas.replicas}") short replicas) {
        return TopicBuilder.name(topico + sufijo).partitions(particiones).replicas(replicas).build();
    }
}
