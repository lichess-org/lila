package lila.opening

import chess.format.{ FEN, Forsyth }
import chess.{ Situation, Speed }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import lila.common.{ LilaOpening, LilaOpeningFamily }

case class OpeningQuery(fen: FEN, position: Situation, speeds: Set[Speed], ratings: Set[Int]) {
  def variant = chess.variant.Standard
}

object OpeningQuery {

  def parse(fenStr: String): Option[(FEN, Situation)] = {
    val fen = FEN.clean(fenStr)
    Forsyth << fen filter (_ playable true) map { pos =>
      (Forsyth >> pos, pos)
    }
  }

  def justFen(fenStr: String) =
    parse(fenStr) map { case (fen, pos) =>
      OpeningQuery(fen, pos, defaultSpeeds, defaultRatings)
    }

  def byOpening(key: String) = {
    LilaOpening.find(key).map(_.ref) orElse LilaOpeningFamily.find(key).flatMap(_.full)
  }.map(_.fen) flatMap justFen

  val defaultRatings = Set[Int](1600, 1800, 2000, 2200, 2500)
  val defaultSpeeds =
    Set[Speed](Speed.Bullet, Speed.Blitz, Speed.Rapid, Speed.Classical, Speed.Correspondence)

  val firstYear  = 2016
  val firstMonth = s"$firstYear-01"
  def lastMonth =
    DateTimeFormat forPattern "yyyy-MM" print {
      val now = DateTime.now
      if (now.dayOfMonth.get > 7) now else now.minusMonths(1)
    }
}
