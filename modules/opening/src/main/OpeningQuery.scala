package lila.opening

import chess.format.{ FEN, Forsyth }
import chess.opening.FullOpeningDB
import chess.variant.Standard
import chess.{ Situation, Speed }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lila.common.{ LilaOpening, LilaOpeningFamily }

case class OpeningQuery(pgn: String, position: Situation, speeds: Set[Speed], ratings: Set[Int]) {
  def variant = chess.variant.Standard
  val fen     = Forsyth >> position
  val opening = FullOpeningDB.findByFen(fen).flatMap(LilaOpening.apply)
  val name    = opening.fold(pgn)(_.ref.name)
  val key     = opening.fold(pgn.replace(" ", "_"))(_.key.value)
}

object OpeningQuery {

  println(lila.common.LilaOpeningFamily.find("Sicilian_Defense").get.full.get.pgn)

  def fromPgn(pgn: String) = for {
    parsed <- chess.format.pgn.Reader.full(pgn).toOption
    replay <- parsed.valid.toOption
    game = replay.state
    sit  = game.situation
    if sit playable true
  } yield OpeningQuery(game.pgnMoves mkString " ", sit, defaultSpeeds, defaultRatings)

  def byOpening(key: String) = {
    LilaOpening.find(key).map(_.ref) orElse LilaOpeningFamily.find(key).flatMap(_.full)
  }.map(_.pgn) flatMap fromPgn

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
