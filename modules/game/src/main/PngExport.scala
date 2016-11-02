package lila.game

import play.api.libs.iteratee._
import play.api.libs.ws.WS
import play.api.Play.current

import chess.format.Forsyth

private final class PngExport(url: String, size: Int) {

  def apply(game: Game): Fu[Enumerator[Array[Byte]]] = {

    val queryString =
      ("fen" -> (Forsyth >> game.toChess).split(' ').head) :: List(
        game.castleLastMoveTime.lastMoveString.map { "lastMove" -> _ },
        game.toChess.situation.checkSquare.map { "check" -> _.key }
      ).flatten

    WS.url(url).withQueryString(queryString: _*).getStream() flatMap {
      case (res, body) if res.status != 200 =>
        logger.warn(s"PgnExport ${game.id} ${res.status}")
        fufail(res.status.toString)
      case (_, body) => fuccess(body)
    }
  }
}
