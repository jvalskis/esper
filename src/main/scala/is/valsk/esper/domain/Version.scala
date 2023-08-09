package is.valsk.esper.domain

import is.valsk.esper.domain.SemanticVersion.SemanticVersionSegment
import zio.json.{JsonDecoder, JsonEncoder}

import scala.annotation.{tailrec, targetName}

case class Version(
    value: String,
) {
  def >(other: Version)(using ordering: Ordering[Version]): Boolean = ordering.gt(this, other)

  def <(other: Version)(using ordering: Ordering[Version]): Boolean = ordering.lt(this, other)

  def >=(other: Version)(using ordering: Ordering[Version]): Boolean = ordering.gteq(this, other)

  def <=(other: Version)(using ordering: Ordering[Version]): Boolean = ordering.lteq(this, other)

  def ===(other: Version)(using ordering: Ordering[Version]): Boolean = ordering.equiv(this, other)
}

object Version {
  implicit val decoder: JsonDecoder[Version] = JsonDecoder[String].map(Version(_))
  implicit val encoder: JsonEncoder[Version] = JsonEncoder[String].contramap(_.value)

  def unapply(string: String): Option[Version] = Some(Version(string))
}

object SemanticVersion {
  val Ordering: Ordering[Version] = scala.Ordering[Version] { (a, b) => compareSemantically(
    a.value.split("\\.").toList,
    b.value.split("\\.").toList
  )}

  @tailrec
  private def compareSemantically(thisVersion: List[String], otherVersion: List[String]): Int = {
    (thisVersion, otherVersion) match {
      case (Nil, Nil) => 0
      case (Nil, _) => -1
      case (_, Nil) => 1
      case (SemanticVersionSegment(thisNumber, thisSuffix) :: thisTail, SemanticVersionSegment(otherNumber, otherSuffix) :: otherTail) =>
        if (thisNumber > otherNumber) 1
        else if (thisNumber < otherNumber) -1
        else if (thisSuffix > otherSuffix) 1
        else if (thisSuffix < otherSuffix) -1
        else compareSemantically(thisTail, otherTail)
      case (thisString :: thisTail, otherString :: otherTail) =>
        if (thisString > otherString) 1
        else if (thisString < otherString) -1
        else compareSemantically(thisTail, otherTail)
    }
  }

  case class SemanticVersionSegment(
      number: Int,
      suffix: String
  )

  object SemanticVersionSegment {

    def unapply(string: String): Option[SemanticVersionSegment] = {
      val prefix = string.takeWhile(x => !x.isDigit)
      val number = string.drop(prefix.length).takeWhile(_.isDigit)
      val suffix = string.drop(prefix.length + number.length)
      Some(SemanticVersionSegment(number.toIntOption.getOrElse(0), suffix))
    }
  }
}