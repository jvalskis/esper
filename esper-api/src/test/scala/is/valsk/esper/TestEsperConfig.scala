package is.valsk.esper

import is.valsk.esper.config.{EsperConfig, HassConfig, ScheduleConfig, RestServerConfig}
import zio.{ULayer, ZLayer}

object TestEsperConfig {
  val layer: ULayer[EsperConfig] = ZLayer.succeed(EsperConfig(
    server = RestServerConfig(
      host = "localhost",
    ),
    hass = HassConfig(
      webSocketUrl = "ws://localhost:8123/api/websocket",
      accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiIy"
    ),
    schedule = ScheduleConfig(
      initialDelay = 1,
      interval = 1,
      jitter = false,
      maxRetries = 1,
      exponentialRetryBase = 1
    )
  ))
}
