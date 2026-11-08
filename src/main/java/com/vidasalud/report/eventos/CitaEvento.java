package com.vidasalud.report.eventos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Evento de cita que publica ms-appointments en el tópico appointments.events.
 * Es el contrato de lectura de este servicio: solo los campos necesarios para los KPIs.
 * No incluye datos personales del paciente (el reporte no los necesita).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitaEvento(
        String tipo,
        Long citaId,
        Long servicioId,
        Long boxId,
        String estado,
        Instant ocurridoEn) {}
