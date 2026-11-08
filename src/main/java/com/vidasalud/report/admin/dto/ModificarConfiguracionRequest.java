package com.vidasalud.report.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** @param configuracion parámetros a establecer, por ejemplo {"retention.ms": "86400000"} */
public record ModificarConfiguracionRequest(
        @NotNull(message = "es obligatorio")
        @NotEmpty(message = "debe incluir al menos un parámetro")
        Map<String, String> configuracion) {}
