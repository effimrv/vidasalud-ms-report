package com.vidasalud.report.admin;

import com.vidasalud.report.admin.dto.AumentarParticionesRequest;
import com.vidasalud.report.admin.dto.CrearTopicoRequest;
import com.vidasalud.report.admin.dto.GrupoConsumoResponse;
import com.vidasalud.report.admin.dto.LagGrupoResponse;
import com.vidasalud.report.admin.dto.ModificarConfiguracionRequest;
import com.vidasalud.report.admin.dto.TopicoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administración de Kafka (solo rol Admin).
 *
 * <pre>
 * GET    /api/kafka/topics                          lista los tópicos
 * POST   /api/kafka/topics                          crea un tópico (particiones, réplicas, configuración) → 201
 * GET    /api/kafka/topics/{nombre}                 detalle: particiones, réplicas y configuración modificada
 * PATCH  /api/kafka/topics/{nombre}/configuracion   cambia parámetros (retención, cleanup.policy, ...)
 * POST   /api/kafka/topics/{nombre}/particiones     aumenta el número de particiones
 * DELETE /api/kafka/topics/{nombre}                 elimina el tópico → 204
 * GET    /api/kafka/consumer-groups                 lista los consumer groups y su estado
 * GET    /api/kafka/consumer-groups/{grupo}/lag     offset consumido, offset final y lag por partición
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    /** Nombres válidos de tópico en Kafka: letras, números, punto, guion y guion bajo. */
    public static final String PATRON_NOMBRE = "^[a-zA-Z0-9._-]{1,249}$";
    public static final String MENSAJE_NOMBRE = "solo letras, números, '.', '-' o '_' (máximo 249 caracteres)";

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/topics")
    public List<String> listarTopicos() {
        return service.listarTopicos();
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicoResponse crearTopico(@Valid @RequestBody CrearTopicoRequest solicitud) {
        return service.crearTopico(solicitud);
    }

    @GetMapping("/topics/{nombre}")
    public TopicoResponse obtenerTopico(@PathVariable @Pattern(regexp = PATRON_NOMBRE, message = MENSAJE_NOMBRE) String nombre) {
        return service.obtenerTopico(nombre);
    }

    @PatchMapping("/topics/{nombre}/configuracion")
    public TopicoResponse modificarConfiguracion(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE, message = MENSAJE_NOMBRE) String nombre,
            @Valid @RequestBody ModificarConfiguracionRequest solicitud) {
        return service.modificarConfiguracion(nombre, solicitud);
    }

    @PostMapping("/topics/{nombre}/particiones")
    public TopicoResponse aumentarParticiones(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE, message = MENSAJE_NOMBRE) String nombre,
            @Valid @RequestBody AumentarParticionesRequest solicitud) {
        return service.aumentarParticiones(nombre, solicitud);
    }

    @DeleteMapping("/topics/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTopico(@PathVariable @Pattern(regexp = PATRON_NOMBRE, message = MENSAJE_NOMBRE) String nombre) {
        service.eliminarTopico(nombre);
    }

    @GetMapping("/consumer-groups")
    public List<GrupoConsumoResponse> listarGrupos() {
        return service.listarGruposDeConsumo();
    }

    @GetMapping("/consumer-groups/{grupo}/lag")
    public LagGrupoResponse lagDelGrupo(@PathVariable @Pattern(regexp = PATRON_NOMBRE, message = MENSAJE_NOMBRE) String grupo) {
        return service.lagDelGrupo(grupo);
    }
}
