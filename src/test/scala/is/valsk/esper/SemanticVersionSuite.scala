package is.valsk.esper

import io.circe
import io.circe.Decoder
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.generic.semiauto.deriveDecoder
import is.valsk.esper.hass.messages.Type.*
import is.valsk.esper.hass.messages.responses.AuthRequired.decoder
import zio.json.*
import io.circe.syntax.*
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthRequired, Result}
import is.valsk.esper.utils.SemanticVersion
import munit.Suite
import zio.ZIO
import zio.test.*
import zio.test.Assertion.*

object SemanticVersionSuite extends ZIOSpecDefault {

  def spec = suite("Semantic version comparison")(ZIO.succeed(
    List(
      ("v1.0.0", "1.0.0", 0),
      ("v1.0.0", "v1.0.0", 0),
      ("1.0.0", "1.0.0", 0),
      ("1.0.0", "1.0.1", -1),
      ("1.0.1", "1.0.0", 1),
      ("1.0.1", "1.0.10", -1),
      ("1.0.2", "1.0.10", -1),
      ("1.0.10", "1.0.1", 1),
      ("1.0.1-suffix", "1.0.1", 1),
      ("1.0.1-suffix", "1.0.2", -1),
      ("1.0.1-suffix", "1.0.1-suffix1", -1),
    ).map((testCase _).tupled)
  ))

  private def testCase(string1: String, string2: String, expected: Int): Spec[Any, Nothing] = {
    test(s"$string1 compareTo $string2 => $expected") {
      assertTrue(SemanticVersion(string1).compareTo(SemanticVersion(string2)) == expected)
    }
  }
}
