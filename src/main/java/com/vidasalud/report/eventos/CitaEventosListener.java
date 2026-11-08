package com.vidasalud.report.eventos;

import com.vidasalud.report.reporte.ReporteService;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Consumidor del tópico de citas. Solo recibe el evento y delega en {@link ReporteService}:
 * la lógica de negocio no conoce a Kafka. Concurrency 3 = número de particiones.
 * <p>
 * Los KPIs viven en memoria, así que al arrancar cada partición se relee desde el inicio de la
 * retención para reconstruir el estado. Solo la primera asignación de cada partición se rebobina:
 * los rebalanceos posteriores continúan desde el offset confirmado. Es seguro porque el procesamiento es idempotente.
 */
@Component
public class CitaEventosListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(CitaEventosListener.class);

    private final ReporteService reporte;
    private final Set<TopicPartition> reprocesadas = ConcurrentHashMap.newKeySet();

    public CitaEventosListener(ReporteService reporte) {
        this.reporte = reporte;
    }

    @KafkaListener(
            id = "report-citas",
            topics = "${vidasalud.kafka.topicos.citas.nombre}",
            groupId = "${vidasalud.kafka.grupos.reporte}",
            concurrency = "3")
    public void recibir(CitaEvento evento) {
        reporte.registrar(evento);
    }

    @Override
    public void onPartitionsAssigned(@NonNull Map<TopicPartition, Long> asignaciones, @NonNull ConsumerSeekCallback callback) {
        Set<TopicPartition> nuevas = asignaciones.keySet().stream()
                .filter(tp -> reprocesadas.add(tp))
                .collect(Collectors.toSet());
        if (!nuevas.isEmpty()) {
            callback.seekToBeginning(nuevas);
            log.info("Reconstruyendo KPIs desde el inicio de la retención en {}", nuevas);
        }
    }
}
