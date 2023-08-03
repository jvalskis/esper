package is.valsk.esper.services

import is.valsk.esper.device.{DeviceProxy, DeviceProxyRegistry, DeviceStatus, FlashResult}
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.{Device, DeviceApiError, EsperError, Firmware, Version}
import is.valsk.esper.repositories.DeviceRepository
import is.valsk.esper.services.OtaService.OtaServiceLive
import zio.{IO, URLayer, ZLayer}

trait OtaService {
  def getCurrentFirmwareVersion(deviceId: DeviceId): IO[EsperError, Version]

  def getCurrentFirmwareVersion(device: Device): IO[EsperError, Version]

  def flashFirmware(deviceId: DeviceId, firmware: Firmware): IO[EsperError, Unit]

  def flashFirmware(device: Device, firmware: Firmware): IO[EsperError, FlashResult]

  def getDeviceStatus(deviceId: DeviceId): IO[EsperError, DeviceStatus]

  def getDeviceStatus(device: Device): IO[EsperError, DeviceStatus]

  def restartDevice(deviceId: DeviceId): IO[EsperError, Unit]

  def restartDevice(device: Device): IO[EsperError, Unit]
}

object OtaService {
  val layer: URLayer[DeviceProxyRegistry with DeviceRepository, OtaService] = ZLayer.fromFunction(OtaServiceLive(_, _))

  private class OtaServiceLive(
      deviceProxyRegistry: DeviceProxyRegistry,
      deviceRepository: DeviceRepository
  ) extends OtaService {

    def getCurrentFirmwareVersion(deviceId: DeviceId): IO[EsperError, Version] = for {
      device <- deviceRepository.get(deviceId)
      version <- getCurrentFirmwareVersion(device)
    } yield version

    override def getCurrentFirmwareVersion(device: Device): IO[EsperError, Version] = for {
      deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      version <- deviceProxy.getCurrentFirmwareVersion(device)
    } yield version

    def flashFirmware(deviceId: DeviceId, firmware: Firmware): IO[EsperError, Unit] = for {
      device <- deviceRepository.get(deviceId)
      _ <- flashFirmware(device, firmware)
    } yield ()

    override def flashFirmware(device: Device, firmware: Firmware): IO[EsperError, FlashResult] = for {
      deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      result <- deviceProxy.flashFirmware(device, firmware)
    } yield result

    def getDeviceStatus(deviceId: DeviceId): IO[EsperError, DeviceStatus] = for {
      device <- deviceRepository.get(deviceId)
      deviceStatus <- getDeviceStatus(device)
    } yield deviceStatus

    override def getDeviceStatus(device: Device): IO[EsperError, DeviceStatus] = for {
      deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      deviceStatus <- deviceProxy.getDeviceStatus(device)
    } yield deviceStatus

    override def restartDevice(deviceId: DeviceId): IO[EsperError, Unit] = for {
      device <- deviceRepository.get(deviceId)
      _ <- restartDevice(device)
    } yield ()

    override def restartDevice(device: Device): IO[EsperError, Unit] = for {
      deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      _ <- deviceProxy.restartDevice(device)
    } yield ()
  }
}
