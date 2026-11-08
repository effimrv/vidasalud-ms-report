package com.vidasalud.report.admin.dto;

/**
 * @param topico           tópico
 * @param particion        número de partición
 * @param offsetConsumido  último offset confirmado por el grupo
 * @param offsetFinal      offset del último mensaje escrito en la partición
 * @param lag              mensajes pendientes de consumir (offsetFinal - offsetConsumido)
 */
public record LagParticion(String topico, int particion, long offsetConsumido, long offsetFinal, long lag) {}
