package lila.game

import akka.stream.scaladsl.{ Source => StreamSource }
import akka.util.ByteString
import old.play.Env.WS

import chess.format.{ Forsyth, FEN, Uci }

final class PngExport(url: String, size: Int) {

  def fromGame(game: Game): Fu[StreamSource[ByteString, _]] = apply(
    fen = FEN(Forsyth >> game.chess),
    lastMove = game.lastMoveKeys,
    check = game.situation.checkSquare,
    orientation = game.firstColor.some,
    logHint = s"game ${game.id}"
  )

  def apply(
    fen: FEN,
    lastMove: Option[String],
    check: Option[chess.Pos],
    orientation: Option[chess.Color],
    logHint: => String
  ): Fu[StreamSource[ByteString, _]] = {

    val queryString = List(
      "fen" -> fen.value.takeWhile(' ' !=),
      "size" -> size.toString
    ) ::: List(
        lastMove.map { "lastMove" -> _ },
        check.map { "check" -> _.key },
        orientation.map { "orientation" -> _.name }
      ).flatten

    WS.url(url).addQueryStringParameters(queryString: _*).stream() flatMap {
      case (res) if res.status != 200 =>
        logger.warn(s"PgnExport $logHint ${fen.value} ${res.status}")
        fufail(res.status.toString)
      case res => fuccess(res.bodyAsSource)
    }
  }
}
