package com.vidasalud.report.admin.dto;

import com.vidasalud.report.admin.KafkaAdminController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * @param nombre        nombre del tópico
 * @param particiones   número de particiones (paralelismo)
 * @param replicas      factor de replicación (copias por partición)
 * @param configuracion parámetros de administración, por ejemplo retention.ms, cleanup.policy, min.insync.replicas
 */
public record CrearTopicoRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = KafkaAdminController.PATRON_NOMBRE, message = KafkaAdminController.MENSAJE_NOMBRE)
        String nombre,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 100, message = "no puede superar 100")
        Integer particiones,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 3, message = "no puede superar 3 (el clúster tiene 3 brokers)")
        Integer replicas,

        Map<String, String> configuracion) {}
