package com.vidasalud.report.eventos;

import com.vidasalud.report.reporte.ReporteService;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Consumidor de audit.timeline: cuenta los eventos auditados por tipo para los indicadores de auditoría.
 * Como el conteo vive en memoria, cada partición se relee desde el inicio de la retención la primera vez
 * que se asigna; las entradas repetidas se descartan por clave.
 */
@Component
public class TimelineListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(TimelineListener.class);

    private final ReporteService reporte;
    private final Set<TopicPartition> reprocesadas = ConcurrentHashMap.newKeySet();

    public TimelineListener(ReporteService reporte) {
        this.reporte = reporte;
    }

    @KafkaListener(
            id = "report-timeline",
            topics = "${vidasalud.kafka.topicos.timeline.nombre}",
            groupId = "${vidasalud.kafka.grupos.reporteTimeline}",
            containerFactory = "timelineContainerFactory",
            concurrency = "3")
    public void recibir(EntradaAuditoria entrada) {
        reporte.registrarAuditoria(entrada);
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> asignaciones, ConsumerSeekCallback callback) {
        Set<TopicPartition> nuevas = asignaciones.keySet().stream()
                .filter(reprocesadas::add)
                .collect(Collectors.toSet());
        if (!nuevas.isEmpty()) {
            callback.seekToBeginning(nuevas);
            log.info("Reconstruyendo conteo de auditoría desde el inicio de la retención en {}", nuevas);
        }
    }
}
