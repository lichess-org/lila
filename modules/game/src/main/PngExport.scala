package lila.game

import play.api.libs.iteratee._
import play.api.libs.ws.WS
import play.api.Play.current

import chess.format.{ Forsyth, FEN }

final class PngExport(url: String, size: Int) {

  def fromGame(game: Game): Fu[Enumerator[Array[Byte]]] = apply(
    fen = FEN(Forsyth >> game.toChess),
    lastMove = game.castleLastMoveTime.lastMoveString,
    check = game.toChess.situation.checkSquare,
    orientation = game.firstColor.some,
    logHint = s"game ${game.id}")

  def apply(
    fen: FEN,
    lastMove: Option[String],
    check: Option[chess.Pos],
    orientation: Option[chess.Color],
    logHint: => String): Fu[Enumerator[Array[Byte]]] = {

    val queryString = List(
      "fen" -> fen.value.takeWhile(' ' !=),
      "size" -> size.toString
    ) ::: List(
        lastMove.map { "lastMove" -> _ },
        check.map { "check" -> _.key },
        orientation.map { "orientation" -> _.name }
      ).flatten

    WS.url(url).withQueryString(queryString: _*).getStream() flatMap {
      case (res, body) if res.status != 200 =>
        logger.warn(s"PgnExport $logHint ${fen.value} ${res.status}")
        fufail(res.status.toString)
      case (_, body) => fuccess(body)
    }
  }
}
