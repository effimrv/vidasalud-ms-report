package com.vidasalud.report.admin.dto;

import java.util.List;

/** @param lagTotal suma del lag de todas las particiones: crecer de forma sostenida indica un consumidor lento. */
public record LagGrupoResponse(String grupo, List<LagParticion> particiones, long lagTotal) {}
