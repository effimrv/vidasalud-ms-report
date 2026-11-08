package com.vidasalud.report.reporte;

import com.vidasalud.report.eventos.CitaEvento;
import com.vidasalud.report.eventos.EntradaAuditoria;
import com.vidasalud.report.eventos.MensajeInvalidoException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Agregación de KPIs a partir de los eventos de citas. Guarda el último estado conocido de cada cita,
 * así procesar dos veces el mismo evento (entrega "al menos una vez") no duplica conteos.
 */
@Service
public class ReporteService {

    private final Map<Long, CitaResumen> citas = new ConcurrentHashMap<>();
    private final AtomicLong eventosProcesados = new AtomicLong();
    /** Claves de las entradas de auditoría ya contadas (citaId:tipo:instante), para no contar dos veces. */
    private final Set<String> entradasAuditadas = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> auditadasPorTipo = new ConcurrentHashMap<>();

    private record CitaResumen(String estado, Long servicioId, Instant ocurridoEn) {}

    public void registrar(CitaEvento evento) {
        if (evento == null || evento.citaId() == null || evento.estado() == null) {
            throw new MensajeInvalidoException("Evento sin citaId o estado: " + evento);
        }
        citas.merge(evento.citaId(), new CitaResumen(evento.estado(), evento.servicioId(), evento.ocurridoEn()),
                (actual, nuevo) -> masReciente(actual, nuevo));
        eventosProcesados.incrementAndGet();
    }

    private static CitaResumen masReciente(CitaResumen actual, CitaResumen nuevo) {
        if (actual.ocurridoEn() != null && nuevo.ocurridoEn() != null && nuevo.ocurridoEn().isBefore(actual.ocurridoEn())) {
            return actual;
        }
        return nuevo;
    }

    /** Cuenta una entrada del timeline de auditoría una sola vez por clave. */
    public void registrarAuditoria(EntradaAuditoria entrada) {
        if (entrada == null || entrada.citaId() == null || entrada.tipo() == null) {
            throw new MensajeInvalidoException("Entrada de auditoría sin citaId o tipo: " + entrada);
        }
        String clave = entrada.citaId() + ":" + entrada.tipo() + ":" + entrada.ocurridoEn();
        if (entradasAuditadas.add(clave)) {
            auditadasPorTipo.merge(entrada.tipo(), 1L, (a, b) -> a + b);
        }
    }

    /** Eventos del timeline por tipo (cuántas veces se registró cada cambio de estado). */
    public ResumenAuditoria auditoria() {
        Map<String, Long> porTipo = new TreeMap<>(auditadasPorTipo);
        long total = porTipo.values().stream().mapToLong(cuenta -> cuenta).sum();
        return new ResumenAuditoria(total, porTipo);
    }

    public Kpis kpis() {
        Map<String, Long> porEstado = citas.values().stream()
                .collect(Collectors.groupingBy(resumen -> resumen.estado(), Collectors.counting()));
        long total = citas.size();
        long canceladas = porEstado.getOrDefault("CANCELADA", 0L);
        double tasaCancelacion = total == 0 ? 0.0 : Math.round(canceladas * 1000.0 / total) / 10.0;
        return new Kpis(total, porEstado, tasaCancelacion, eventosProcesados.get());
    }

    /** Servicios ordenados por cantidad de citas (de mayor a menor). */
    public List<ConteoServicio> topServicios(int limite) {
        return citas.values().stream()
                .filter(resumen -> resumen.servicioId() != null)
                .collect(Collectors.groupingBy(resumen -> resumen.servicioId(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ConteoServicio(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong((ConteoServicio c) -> c.citas()).reversed()
                        .thenComparing(c -> c.servicioId()))
                .limit(limite)
                .toList();
    }

    /** @param tasaCancelacion porcentaje de citas canceladas sobre el total (0 a 100). */
    public record Kpis(long totalCitas, Map<String, Long> porEstado, double tasaCancelacion, long eventosProcesados) {}

    public record ConteoServicio(Long servicioId, long citas) {}

    public record ResumenAuditoria(long totalEventos, Map<String, Long> porTipo) {}
}
