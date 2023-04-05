package is.valsk.esper.device.shelly

import is.valsk.esper.device.shelly.ShellyDevice.ShellyFirmwareEntry
import is.valsk.esper.domain.SemanticVersion
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object ShellyFirmwareEntrySuite extends ZIOSpecDefault {

  def spec = test("Deserialize to ShellyFirmwareVersionEntry") {
    val json =
      """
      {
        "version": "v1.10.2",
        "file": "SHDW-2.zip"
      }
      """

    assertTrue(json.fromJson[ShellyFirmwareEntry] == Right(ShellyFirmwareEntry(SemanticVersion("v1.10.2"), "SHDW-2.zip")))
  }
}
