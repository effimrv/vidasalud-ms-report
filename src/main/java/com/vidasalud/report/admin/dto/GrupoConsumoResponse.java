package com.vidasalud.report.admin.dto;

/** @param grupo nombre del consumer group; @param estado STABLE, EMPTY, DEAD, PREPARING_REBALANCE, ... */
public record GrupoConsumoResponse(String grupo, String estado) {}
