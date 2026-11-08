package com.vidasalud.report.eventos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Entrada del timeline que publica ms-audit en audit.timeline. Es el contrato de lectura de este servicio:
 * solo los campos que usan los indicadores de auditoría.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntradaAuditoria(
        Long citaId,
        String tipo,
        String estado,
        Long servicioId,
        Long boxId,
        Instant ocurridoEn,
        int particion,
        long offset) {}
