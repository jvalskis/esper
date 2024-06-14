package is.valsk.esper

import zio.{ULayer, ZLayer}

object TestEsperConfig {
  val layer: ULayer[EsperConfig] = ZLayer.succeed(EsperConfig(
    host = "localhost",
    hassConfig = HassConfig(
      webSocketUrl = "ws://localhost:8123/api/websocket",
      accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiIy"
    ),
    scheduleConfig = ScheduleConfig(
      initialDelay = 1,
      interval = 1,
      jitter = false,
      maxRetries = 1,
      exponentialRetryBase = 1
    )
  ))
}
