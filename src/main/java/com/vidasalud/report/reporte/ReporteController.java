package com.vidasalud.report.reporte;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de reportería (solo rol Admin).
 *
 * <pre>
 * GET /api/report/kpis                 total de citas, conteo por estado, tasa de cancelación
 * GET /api/report/top-services?limite=5  servicios con más citas
 * GET /api/report/auditoria            eventos del timeline de auditoría por tipo (audit.timeline)
 * </pre>
 */
@RestController
@RequestMapping("/api/report")
public class ReporteController {

    private final ReporteService reporte;

    public ReporteController(ReporteService reporte) {
        this.reporte = reporte;
    }

    @GetMapping("/kpis")
    public ReporteService.Kpis kpis() {
        return reporte.kpis();
    }

    @GetMapping("/auditoria")
    public ReporteService.ResumenAuditoria auditoria() {
        return reporte.auditoria();
    }

    @GetMapping("/top-services")
    public List<ReporteService.ConteoServicio> topServicios(@RequestParam(defaultValue = "5") int limite) {
        return reporte.topServicios(Math.max(1, Math.min(limite, 50)));
    }
}
