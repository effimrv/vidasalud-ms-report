package com.vidasalud.report.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** @param particiones nuevo total de particiones; debe ser mayor que el actual (Kafka no las reduce). */
public record AumentarParticionesRequest(
        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 100, message = "no puede superar 100")
        Integer particiones) {}
