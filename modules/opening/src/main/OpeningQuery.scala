package lila.opening

import chess.format.{ FEN, Forsyth }
import chess.opening.FullOpeningDB
import chess.variant.Standard
import chess.{ Situation, Speed }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lila.common.LilaOpeningFamily

case class OpeningQuery(pgn: Vector[String], position: Situation, speeds: Set[Speed], ratings: Set[Int]) {
  def variant           = chess.variant.Standard
  val fen               = Forsyth >> position
  val opening           = FullOpeningDB findByFen fen
  val openingIfShortest = opening filter Opening.isShortest
  val family            = opening.map(_.family)
  def pgnString         = pgn mkString " "
  val name              = opening.fold(pgnString)(_.name)
  val key               = openingIfShortest.fold(pgn mkString "_")(_.key)
  def initial           = pgn.isEmpty
  def prev              = pgn.init.nonEmpty ?? OpeningQuery(pgn.init mkString " ")

  override def toString = s"$pgn $opening"
}

object OpeningQuery {

  def apply(q: String): Option[OpeningQuery] = byOpening(q) orElse fromPgn(q.replace("_", " "))

  private def byOpening(key: String) =
    Opening.shortestLines.get(key).map(_.pgn) flatMap fromPgn

  private def fromPgn(pgn: String) = for {
    parsed <- chess.format.pgn.Reader.full(pgn).toOption
    replay <- parsed.valid.toOption
    game = replay.state
    sit  = game.situation
    if sit playable true
  } yield OpeningQuery(game.pgnMoves, sit, defaultSpeeds, defaultRatings)

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
