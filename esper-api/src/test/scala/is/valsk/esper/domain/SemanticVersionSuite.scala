package is.valsk.esper.domain

import is.valsk.esper.domain.SemanticVersion
import zio.ZIO
import zio.test.*
import zio.test.Assertion.*

object SemanticVersionSuite extends ZIOSpecDefault {

  def spec = suite("Semantic version comparison")(ZIO.succeed(
    List(
      ("v1.0.0", "1.0.0", 0),
      ("v1.0.0", "v1.0.0", 0),
      ("1.0.0", "1.0.0", 0),
      ("1.0", "1.0.0", -1),
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
      assertTrue(SemanticVersion.Ordering.compare(Version(string1), Version(string2)) == expected)
    }
  }
}
