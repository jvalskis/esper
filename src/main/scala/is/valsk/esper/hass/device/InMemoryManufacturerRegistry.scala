package is.valsk.esper.hass.device

import is.valsk.esper.device.ManufacturerRegistry
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.device.DeviceManufacturerHandler.Manufacturer
import zio.*

class InMemoryManufacturerRegistry(deviceMap: Ref[Map[Manufacturer, DeviceManufacturerHandler]]) extends ManufacturerRegistry {

  def findHandler(manufacturer: Manufacturer): UIO[Option[DeviceManufacturerHandler]] = deviceMap.get.map(_.get(manufacturer))
}

object InMemoryManufacturerRegistry {

  val layer: URLayer[Map[Manufacturer, DeviceManufacturerHandler], ManufacturerRegistry] = ZLayer {
    for {
      manufacturerHandlerMap <- ZIO.service[Map[Manufacturer, DeviceManufacturerHandler]]
      ref <- Ref.make(manufacturerHandlerMap)
    } yield InMemoryManufacturerRegistry(ref)
  }
}