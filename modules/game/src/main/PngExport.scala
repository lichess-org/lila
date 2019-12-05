package lila.game

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.ws.WSClient

import chess.format.{ Forsyth, FEN }

final class PngExport(
    ws: WSClient,
    url: String,
    size: Int
) {

  def fromGame(game: Game): Fu[Source[ByteString, _]] = apply(
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
  ): Fu[Source[ByteString, _]] = {

    val queryString = List(
      "fen" -> fen.value.takeWhile(' ' !=),
      "size" -> size.toString
    ) ::: List(
        lastMove.map { "lastMove" -> _ },
        check.map { "check" -> _.key },
        orientation.map { "orientation" -> _.name }
      ).flatten

    ws.url(url)
      .withQueryStringParameters(queryString: _*)
      .withMethod("GET")
      .stream() flatMap {
        case res if res.status != 200 =>
          logger.warn(s"PgnExport $logHint ${fen.value} ${res.status}")
          fufail(res.status.toString)
        case res => fuccess(res.bodyAsSource)
      }
  }
}
