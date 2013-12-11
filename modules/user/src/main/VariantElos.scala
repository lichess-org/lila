package lila.user

import chess.Variant
import play.api.libs.json._

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

  import lila.db.BSON
  import reactivemongo.bson.BSONDocument

  private def variantElosBSONHandler = new BSON[VariantElos] {

    implicit def subEloHandler = SubElo.tube.handler

    def reads(r: BSON.Reader): VariantElos = VariantElos(
      standard = r.getO[SubElo]("standard") | default.standard,
      chess960 = r.getO[SubElo]("chess960") | default.chess960)

    def writes(w: BSON.Writer, o: VariantElos) = BSONDocument(
      "standard" -> o.standard,
      "chess960" -> o.chess960)
  }

  private[user] lazy val tube = lila.db.BsTube(variantElosBSONHandler)
}
