package lila.user

import chess.Variant
import play.api.libs.json._

import lila.db.JsTube
import lila.db.JsTube.Helpers._

case class VariantElos(
    standard: SubElo,
    chess960: SubElo) {

  def apply(variant: Variant) = variant match {
    case Variant.Chess960 ⇒ chess960
    case _                ⇒ standard
  }

  def toMap = Map(
    Variant.Standard -> standard,
    Variant.Chess960 -> chess960)

  def addGame(variant: Variant, newElo: Int) = variant match {
    case Variant.Chess960 ⇒ copy(chess960 = chess960 addGame newElo)
    case Variant.Standard ⇒ copy(standard = standard addGame newElo)
    case _                ⇒ this
  }

  def adjustTo(to: Int) = {
    val nb = toMap.values.map(_.nb).sum
    if (nb == 0) this else {
      val median = (toMap.values map {
        case SubElo(nb, elo) ⇒ nb * elo
      }).sum / nb
      val diff = to - median
      def amortize(se: SubElo) = se withElo (se.elo + (diff * se.nb / nb))
      VariantElos(
        standard = amortize(standard),
        chess960 = amortize(chess960))
    }
  }
}

object VariantElos {

  val default = VariantElos(SubElo.default, SubElo.default)

  private implicit def subEloTube = SubElo.tube

  private[user] lazy val tube = JsTube[VariantElos](
    __.json update merge(defaults) andThen Json.reads[VariantElos],
    Json.writes[VariantElos])

  private def defaults = Json.obj(
    "standard" -> SubElo.default,
    "chess960" -> SubElo.default)
}
