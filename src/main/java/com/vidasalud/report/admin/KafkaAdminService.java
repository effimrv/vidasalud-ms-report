package com.vidasalud.report.admin;

import com.vidasalud.report.admin.dto.AumentarParticionesRequest;
import com.vidasalud.report.admin.dto.CrearTopicoRequest;
import com.vidasalud.report.admin.dto.GrupoConsumoResponse;
import com.vidasalud.report.admin.dto.LagGrupoResponse;
import com.vidasalud.report.admin.dto.LagParticion;
import com.vidasalud.report.admin.dto.ModificarConfiguracionRequest;
import com.vidasalud.report.admin.dto.TopicoResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Administración de Kafka sobre el clúster: tópicos (crear, consultar, modificar configuración,
 * aumentar particiones, eliminar) y consumer groups (estado y lag). Usa el AdminClient con la
 * misma configuración de conexión que el resto del servicio.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_SEGUNDOS = 15;

    private final KafkaAdmin kafkaAdmin;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    public List<String> listarTopicos() {
        return ejecutar(admin -> admin.listTopics(new ListTopicsOptions().listInternal(false)).names().get(
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)).stream().sorted().toList();
    }

    public TopicoResponse obtenerTopico(String nombre) {
        return ejecutar(admin -> describir(admin, nombre));
    }

    public TopicoResponse crearTopico(CrearTopicoRequest solicitud) {
        ejecutar(admin -> {
            NewTopic topico = new NewTopic(solicitud.nombre(), solicitud.particiones(), solicitud.replicas().shortValue())
                    .configs(solicitud.configuracion() == null ? Map.of() : solicitud.configuracion());
            admin.createTopics(List.of(topico)).all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            return null;
        });
        log.info("Tópico creado: {} ({} particiones, {} réplicas)", solicitud.nombre(), solicitud.particiones(), solicitud.replicas());
        return obtenerTopico(solicitud.nombre());
    }

    /** Cambia (o agrega) parámetros de configuración del tópico sin recrearlo. */
    public TopicoResponse modificarConfiguracion(String nombre, ModificarConfiguracionRequest solicitud) {
        ejecutar(admin -> {
            ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
            List<AlterConfigOp> operaciones = solicitud.configuracion().entrySet().stream()
                    .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                    .toList();
            admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            return null;
        });
        log.info("Configuración del tópico {} modificada: {}", nombre, solicitud.configuracion().keySet());
        return obtenerTopico(nombre);
    }

    /** Solo se puede aumentar el número de particiones; Kafka no permite reducirlo. */
    public TopicoResponse aumentarParticiones(String nombre, AumentarParticionesRequest solicitud) {
        ejecutar(admin -> {
            admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(solicitud.particiones())))
                    .all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            return null;
        });
        log.info("Tópico {} ahora tiene {} particiones", nombre, solicitud.particiones());
        return obtenerTopico(nombre);
    }

    public void eliminarTopico(String nombre) {
        ejecutar(admin -> {
            admin.deleteTopics(List.of(nombre)).all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            return null;
        });
        log.info("Tópico eliminado: {}", nombre);
    }

    public List<GrupoConsumoResponse> listarGruposDeConsumo() {
        return ejecutar(admin -> admin.listConsumerGroups().all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS).stream()
                .map(g -> new GrupoConsumoResponse(g.groupId(), g.state().map(estado -> estado.name()).orElse("DESCONOCIDO")))
                .sorted(Comparator.comparing(grupo -> grupo.grupo()))
                .toList());
    }

    /** Offset consumido, offset final y lag de cada partición que el grupo tiene asignada. */
    public LagGrupoResponse lagDelGrupo(String grupo) {
        return ejecutar(admin -> {
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> consumidos = admin
                    .listConsumerGroupOffsets(Map.of(grupo, new ListConsumerGroupOffsetsSpec()))
                    .partitionsToOffsetAndMetadata(grupo).get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            if (consumidos.isEmpty()) {
                return new LagGrupoResponse(grupo, List.of(), 0L);
            }
            Map<TopicPartition, OffsetSpec> consultas = consumidos.keySet().stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
            Map<TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo> finales =
                    admin.listOffsets(consultas).all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

            List<LagParticion> particiones = new ArrayList<>();
            consumidos.forEach((tp, om) -> {
                long consumido = om == null ? 0L : om.offset();
                long fin = finales.get(tp).offset();
                particiones.add(new LagParticion(tp.topic(), tp.partition(), consumido, fin, Math.max(0L, fin - consumido)));
            });
            particiones.sort(Comparator.comparing((LagParticion lp) -> lp.topico()).thenComparingInt(lp -> lp.particion()));
            long lagTotal = particiones.stream().mapToLong(lp -> lp.lag()).sum();
            return new LagGrupoResponse(grupo, particiones, lagTotal);
        });
    }

    private TopicoResponse describir(AdminClient admin, String nombre) throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
        TopicDescription descripcion = admin.describeTopics(List.of(nombre))
                .allTopicNames().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS).get(nombre);
        Config config = admin.describeConfigs(List.of(new ConfigResource(ConfigResource.Type.TOPIC, nombre)))
                .all().get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                .values().iterator().next();

        // Solo se muestran los parámetros que alguien cambió; los valores por defecto no aportan al diagnóstico.
        Map<String, String> configuracion = new TreeMap<>();
        for (ConfigEntry entrada : config.entries()) {
            if (entrada.source() != ConfigEntry.ConfigSource.DEFAULT_CONFIG) {
                configuracion.put(entrada.name(), entrada.value());
            }
        }
        int replicas = descripcion.partitions().stream().findFirst().map(info -> info.replicas()).map(nodos -> nodos.size()).orElse(0);
        return new TopicoResponse(nombre, descripcion.partitions().size(), replicas, configuracion);
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(AdminClient admin) throws Exception;
    }

    /** Abre un AdminClient por operación, traduce los errores de Kafka a códigos HTTP y lo cierra al terminar. */
    private <T> T ejecutar(Operacion<T> operacion) {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            return operacion.ejecutar(admin);
        } catch (ExecutionException e) {
            throw traducir(e.getCause() == null ? e : e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operación interrumpida");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw traducir(e);
        }
    }

    private ResponseStatusException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "El tópico o grupo no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "El tópico ya existe");
        }
        if (causa instanceof InvalidConfigurationException || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, causa.getMessage());
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Kafka no respondió: " + (causa == null ? "error desconocido" : causa.getMessage()));
    }
}
