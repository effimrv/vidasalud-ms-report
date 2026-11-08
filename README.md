# vidasalud-ms-report

Consumidor Kafka de los eventos de citas: calcula KPIs y métricas, y administra los tópicos y consumer groups de Kafka.

Puerto `8084`. Tópico consumido: `appointments.events`, grupo `vidasalud-report`. Mensajes no procesables → `appointments.events.DLT`.

## Endpoints (rol Admin, JWT de Entra ID)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/report/kpis` | Total de citas, conteo por estado, tasa de cancelación y eventos procesados |
| GET | `/api/report/top-services?limite=5` | Servicios con más citas |
| GET | `/api/kafka/topics` | Lista los tópicos |
| POST | `/api/kafka/topics` | Crea un tópico (particiones, réplicas, configuración) |
| GET | `/api/kafka/topics/{nombre}` | Detalle del tópico |
| PATCH | `/api/kafka/topics/{nombre}/configuracion` | Modifica parámetros (retención, cleanup policy, ...) |
| POST | `/api/kafka/topics/{nombre}/particiones` | Aumenta particiones |
| DELETE | `/api/kafka/topics/{nombre}` | Elimina un tópico |
| GET | `/api/kafka/consumer-groups` | Lista los consumer groups |
| GET | `/api/kafka/consumer-groups/{grupo}/lag` | Lag por partición |

Detalle completo y justificación del diseño: [mensajeria-kafka.md](https://github.com/effimrv/vidasalud-docs/blob/main/mensajeria-kafka.md) (repo vidasalud-docs).

## Ejecutar

Requiere el clúster Kafka de `vidasalud-infra` (`docker compose -f docker-compose.kafka.yml up -d`).

```bash
mvn test
KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094 SEGURIDAD_HABILITADA=false mvn spring-boot:run
```

`SEGURIDAD_HABILITADA=false` desactiva la validación del JWT. Solo para pruebas locales.
