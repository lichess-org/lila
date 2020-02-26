package lila.game

import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.WSClient

import lila.common.LightUser

import chess.{ Replay, Situation, Game => ChessGame }
import chess.format.{ FEN, Forsyth, Uci }

final class GifExport(
    ws: WSClient,
    lightUserApi: lila.user.LightUserApi,
    url: String
)(implicit ec: scala.concurrent.ExecutionContext) {
  def fromGame(game: Game, initialFen: Option[FEN]): Fu[Source[ByteString, _]] =
    lightUserApi preloadMany game.userIds flatMap { _ =>
      ws.url(url)
        .withMethod("POST")
        .addHttpHeaders("Content-Type" -> "application/json")
        .withBody(
          Json.obj(
            "white"  -> Namer.playerTextBlocking(game.whitePlayer, withRating = true)(lightUserApi.sync),
            "black"  -> Namer.playerTextBlocking(game.blackPlayer, withRating = true)(lightUserApi.sync),
            "delay"  -> 70, // default delay for frames, centis
            "frames" -> frames(game, initialFen)
          )
        )
        .stream() flatMap {
        case res if res.status != 200 =>
          logger.warn(s"GifExport game ${game.id} ${res.status}")
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
        frame(init.situation, None) +: framesRec(games)
    }
  }

  private def framesRec(games: List[(ChessGame, Uci.WithSan)]): JsArray = games match {
    case Nil =>
      Json.arr()
    case (game, Uci.WithSan(uci, _)) :: Nil =>
      // longer delay for last frame
      Json.arr(frame(game.situation, uci.some, delay = Some(500)))
    case (game, Uci.WithSan(uci, _)) :: tail =>
      frame(game.situation, uci.some) +: framesRec(tail)
  }

  private def frame(situation: Situation, uci: Option[Uci], delay: Option[Int] = None) =
    Json
      .obj(
        "fen"      -> (Forsyth >> situation),
        "lastMove" -> uci.map(_.uci)
      )
      .add("check", situation.checkSquare.map(_.key))
      .add("delay", delay)
}
