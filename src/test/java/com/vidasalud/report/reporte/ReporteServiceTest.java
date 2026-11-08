package com.vidasalud.report.reporte;

import com.vidasalud.report.eventos.CitaEvento;
import com.vidasalud.report.eventos.MensajeInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReporteServiceTest {

    private ReporteService reporte;

    @BeforeEach
    void preparar() {
        reporte = new ReporteService();
    }

    @Test
    void cuentaCadaCitaUnaVezConsideradoSuUltimoEstado() {
        reporte.registrar(evento(1L, "CREADA", "SOLICITADA", 1L, "2026-11-08T10:00:00Z"));
        reporte.registrar(evento(1L, "CONFIRMADA", "CONFIRMADA", 1L, "2026-11-08T10:01:00Z"));

        ReporteService.Kpis kpis = reporte.kpis();
        assertThat(kpis.totalCitas()).isEqualTo(1);
        assertThat(kpis.porEstado()).containsEntry("CONFIRMADA", 1L).doesNotContainKey("SOLICITADA");
        assertThat(kpis.eventosProcesados()).isEqualTo(2);
    }

    @Test
    void unEventoRepetidoNoDuplicaElConteo() {
        CitaEvento confirmada = evento(5L, "CONFIRMADA", "CONFIRMADA", 2L, "2026-11-08T10:00:00Z");
        reporte.registrar(confirmada);
        reporte.registrar(confirmada);

        assertThat(reporte.kpis().totalCitas()).isEqualTo(1);
    }

    @Test
    void unEventoAtrasadoNoPisaUnEstadoMasReciente() {
        reporte.registrar(evento(3L, "CERRADA", "CERRADA", 1L, "2026-11-08T12:00:00Z"));
        reporte.registrar(evento(3L, "CONFIRMADA", "CONFIRMADA", 1L, "2026-11-08T10:00:00Z"));

        assertThat(reporte.kpis().porEstado()).containsEntry("CERRADA", 1L);
    }

    @Test
    void calculaLaTasaDeCancelacion() {
        reporte.registrar(evento(1L, "CANCELADA", "CANCELADA", 1L, "2026-11-08T10:00:00Z"));
        reporte.registrar(evento(2L, "CONFIRMADA", "CONFIRMADA", 1L, "2026-11-08T10:00:00Z"));
        reporte.registrar(evento(3L, "CONFIRMADA", "CONFIRMADA", 1L, "2026-11-08T10:00:00Z"));
        reporte.registrar(evento(4L, "CERRADA", "CERRADA", 1L, "2026-11-08T10:00:00Z"));

        assertThat(reporte.kpis().tasaCancelacion()).isEqualTo(25.0);
    }

    @Test
    void sinCitasLaTasaEsCero() {
        assertThat(reporte.kpis().tasaCancelacion()).isZero();
    }

    @Test
    void ordenaLosServiciosPorCantidadDeCitas() {
        reporte.registrar(evento(1L, "CREADA", "SOLICITADA", 7L, "2026-11-08T10:00:00Z"));
        reporte.registrar(evento(2L, "CREADA", "SOLICITADA", 9L, "2026-11-08T10:00:00Z"));
        reporte.registrar(evento(3L, "CREADA", "SOLICITADA", 9L, "2026-11-08T10:00:00Z"));

        assertThat(reporte.topServicios(5))
                .containsExactly(new ReporteService.ConteoServicio(9L, 2), new ReporteService.ConteoServicio(7L, 1));
    }

    @Test
    void unEventoSinCitaIdSeRechazaComoMensajeInvalido() {
        assertThatThrownBy(() -> reporte.registrar(new CitaEvento("CREADA", null, 1L, 1L, "SOLICITADA", Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    private static CitaEvento evento(Long citaId, String tipo, String estado, Long servicioId, String ocurridoEn) {
        return new CitaEvento(tipo, citaId, servicioId, 1L, estado, Instant.parse(ocurridoEn));
    }
}
