package is.valsk.esper.domain

import is.valsk.esper.domain.SemanticVersion.SemanticVersionSegment
import zio.json.{JsonDecoder, JsonEncoder}

import scala.annotation.tailrec

case class SemanticVersion(
    value: String,
) extends Version[SemanticVersion] {

  override def compare(that: SemanticVersion): Int = {
    compareSemantically(value.split("\\.").toList, that.value.split("\\.").toList)
  }

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
}

object SemanticVersion {
  implicit val decoder: JsonDecoder[SemanticVersion] = JsonDecoder[String].map(SemanticVersion(_))
  implicit val encoder: JsonEncoder[SemanticVersion] = JsonEncoder[String].contramap(_.value)

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