package com.vidasalud.report.admin.dto;

import java.util.Map;

/**
 * @param nombre        nombre del tópico
 * @param particiones   número de particiones
 * @param replicas      factor de replicación
 * @param configuracion parámetros modificados respecto de los valores por defecto del clúster
 */
public record TopicoResponse(String nombre, int particiones, int replicas, Map<String, String> configuracion) {}
