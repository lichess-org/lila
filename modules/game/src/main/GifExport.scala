package lila.game

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.WSClient

import chess.{ Replay, Situation }
import chess.format.{ FEN, Forsyth, Uci }

final class GifExport(
    ws: WSClient,
    url: String
)(implicit ec: scala.concurrent.ExecutionContext) {
  def fromGame(game: Game, initialFen: Option[FEN]): Fu[Source[ByteString, _]] = {
    ws.url(url)
      .withMethod("POST")
      .addHttpHeaders("Content-Type" -> "application/json")
      .withBody(
        Json.obj(
          "white"  -> game.whitePlayer.name,
          "black"  -> game.blackPlayer.name,
          "delay"  -> 50, // default delay for frames, centis
          "frames" -> frames(game, initialFen)
        )
      )
      .stream() flatMap {
      case res if res.status != 200 =>
        logger.warn(s"GifExport game ${game.id} ${res.status} ${res}")
        fufail(res.statusText)
      case res => fuccess(res.bodyAsSource)
    }
  }

  private def frames(game: Game, initialFen: Option[FEN]) = {
    Replay.gameMoveWhileValid(
      game.pgnMoves,
      initialFen.map(_.value) | game.variant.initialFen,
      game.variant
    ) match {
      case (init, games, _) =>
        games.zipWithIndex.foldLeft(Json.arr(frame(init.situation, None, None))) {
          case (acc, ((g, Uci.WithSan(uci, _)), i)) =>
            acc :+ frame(g.situation, uci.some, Some(500).ifTrue(i + 1 == games.size))
        }
    }
  }

  private def frame(situation: Situation, uci: Option[Uci], delay: Option[Int]) =
    Json
      .obj(
        "fen"      -> (Forsyth >> situation),
        "lastMove" -> uci.map(_.uci)
      )
      .add("check", situation.checkSquare.map(_.key))
      .add("delay", delay)
}
