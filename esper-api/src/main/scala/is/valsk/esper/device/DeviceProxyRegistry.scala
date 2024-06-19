package is.valsk.esper.device

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.Manufacturer
import zio.{IO, URLayer, ZIO, ZLayer}

trait DeviceProxyRegistry {

  def selectProxy(manufacturer: Manufacturer): IO[ManufacturerNotSupported, DeviceProxy]
}

object DeviceProxyRegistry {

  private class DeviceProxyRegistryLive(
      deviceProxyMap: Map[Manufacturer, DeviceProxy]
  ) extends DeviceProxyRegistry {

    override def selectProxy(manufacturer: Manufacturer): IO[ManufacturerNotSupported, DeviceProxy] =
      deviceProxyMap
        .get(manufacturer)
        .map(ZIO.succeed)
        .getOrElse(ZIO.fail(ManufacturerNotSupported(manufacturer)))
  }

  val layer: URLayer[Seq[DeviceHandler], DeviceProxyRegistry] = ZLayer {
    for {
      deviceHandlers <- ZIO.service[Seq[DeviceHandler]]
    } yield new DeviceProxyRegistryLive(deviceHandlers.map(handler => handler.supportedManufacturer -> handler).toMap)
  }
}