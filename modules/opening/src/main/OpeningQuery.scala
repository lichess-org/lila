package lila.opening

import chess.format.{ FEN, Forsyth, Uci }
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.Replay
import chess.variant.Standard
import chess.{ Situation, Speed }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lila.common.LilaOpeningFamily

case class OpeningQuery(replay: Replay, config: OpeningConfig) {
  val pgn: Vector[String] = replay.state.pgnMoves
  val uci: Vector[Uci]    = replay.moves.view.map(_.fold(_.toUci, _.toUci)).reverse.toVector
  def position            = replay.state.situation
  def variant             = chess.variant.Standard
  val fen                 = Forsyth >> replay.state.situation
  val opening             = FullOpeningDB findByFen fen
  val openingIfShortest   = opening filter Opening.isShortest
  val family              = opening.map(_.family)
  def pgnString           = pgn mkString " "
  val key                 = openingIfShortest.fold(pgn mkString "_")(_.key)
  def initial             = pgn.isEmpty
  def prev                = (pgn.sizeIs > 1) ?? OpeningQuery(pgn.init mkString " ", config)

  val openingAndExtraMoves: (Option[FullOpening], List[String]) =
    opening.map(_.some -> Nil) orElse FullOpeningDB.search(replay).map { case FullOpening.AtPly(op, ply) =>
      op.some -> pgn.drop(ply).toList
    } getOrElse (none, pgn.toList)

  val name = openingAndExtraMoves match {
    case (Some(op), Nil)   => op.name
    case (Some(op), moves) => s"${op.name}, ${moves mkString " "}"
    case (_, moves)        => moves mkString " "
  }

  override def toString = s"$pgn $opening"
}

object OpeningQuery {

  def apply(q: String, config: OpeningConfig): Option[OpeningQuery] =
    byOpening(q, config) orElse fromPgn(q.replace("_", " "), config)

  private def byOpening(key: String, config: OpeningConfig) =
    Opening.shortestLines.get(key).map(_.pgn) flatMap { fromPgn(_, config) }

  private def fromPgn(pgn: String, config: OpeningConfig) = for {
    parsed <- chess.format.pgn.Reader.full(pgn).toOption
    replay <- parsed.valid.toOption
    if replay.state.situation playable true
  } yield OpeningQuery(
    replay,
    config
  )

  val firstYear  = 2017
  val firstMonth = s"$firstYear-04"
  // def lastMonth =
  //   DateTimeFormat forPattern "yyyy-MM" print {
  //     val now = DateTime.now
  //     if (now.dayOfMonth.get > 7) now else now.minusMonths(1)
  //   }
}
