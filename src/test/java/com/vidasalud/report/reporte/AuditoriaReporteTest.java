package com.vidasalud.report.reporte;

import com.vidasalud.report.eventos.EntradaAuditoria;
import com.vidasalud.report.eventos.MensajeInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditoriaReporteTest {

    @Test
    void contaLosEventosPorTipo() {
        ReporteService reporte = new ReporteService();
        reporte.registrarAuditoria(entrada(1L, "CREADA", "2026-11-08T10:00:00Z"));
        reporte.registrarAuditoria(entrada(1L, "CONFIRMADA", "2026-11-08T10:01:00Z"));
        reporte.registrarAuditoria(entrada(2L, "CREADA", "2026-11-08T10:02:00Z"));

        ReporteService.ResumenAuditoria resumen = reporte.auditoria();
        assertThat(resumen.totalEventos()).isEqualTo(3);
        assertThat(resumen.porTipo()).containsEntry("CREADA", 2L).containsEntry("CONFIRMADA", 1L);
    }

    @Test
    void unaEntradaRepetidaSeCuentaUnaSolaVez() {
        ReporteService reporte = new ReporteService();
        EntradaAuditoria e = entrada(5L, "CERRADA", "2026-11-08T11:00:00Z");
        reporte.registrarAuditoria(e);
        reporte.registrarAuditoria(e);

        assertThat(reporte.auditoria().totalEventos()).isEqualTo(1);
    }

    @Test
    void unaEntradaSinCitaSeRechaza() {
        ReporteService reporte = new ReporteService();
        assertThatThrownBy(() -> reporte.registrarAuditoria(new EntradaAuditoria(null, "CREADA", null, null, null,
                Instant.now(), 0, 0L))).isInstanceOf(MensajeInvalidoException.class);
    }

    private static EntradaAuditoria entrada(Long citaId, String tipo, String instante) {
        return new EntradaAuditoria(citaId, tipo, tipo, 1L, null, Instant.parse(instante), 0, 0L);
    }
}
