package lila.push

import akka.actor._
import akka.pattern.ask
import chess.format.Forsyth
import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov, Namer }
import lila.hub.actorApi.map.Ask
import lila.hub.actorApi.round.{ MoveEvent, IsOnGame }
import lila.user.User

import play.api.libs.json._

private final class PushApi(
    googlePush: GooglePush,
    implicit val lightUser: String => Option[LightUser],
    roundSocketHub: ActorSelection) {

  def finish(game: Game): Funit =
    if (!game.isCorrespondence || game.hasAi) funit
    else game.userIds.map { userId =>
      Pov.ofUserId(game, userId) ?? { pov =>
        IfAway(pov) {
          googlePush(userId) {
            GooglePush.Data(
              title = pov.win match {
                case Some(true)  => "You won!"
                case Some(false) => "You lost."
                case _           => "It's a draw."
              },
              body = s"Your game with ${opponentName(pov)} is over.",
              payload = Json.obj(
                "userId" -> userId,
                "userData" -> Json.obj(
                  "gameId" -> game.id,
                  "fullId" -> pov.fullId,
                  "color" -> pov.color.name,
                  "fen" -> Forsyth.exportBoard(game.toChess.board),
                  "lastMove" -> game.castleLastMoveTime.lastMoveString,
                  "win" -> pov.win)
              ))
          }
        }
      }
    }.sequenceFu.void

  def move(move: MoveEvent): Funit = move.opponentUserId ?? { userId =>
    def filter(g: Game) = g.isCorrespondence && g.playable && g.nonAi
    GameRepo game move.gameId flatMap {
      case Some(game) if filter(game) =>
        game.pgnMoves.lastOption ?? { sanMove =>
          Pov.ofUserId(game, userId) ?? { pov =>
            IfAway(pov) {
              googlePush(userId) {
                GooglePush.Data(
                  title = "It's your turn!",
                  body = s"${opponentName(pov)} played $sanMove",
                  payload = Json.obj(
                    "userId" -> userId,
                    "userData" -> Json.obj(
                      "gameId" -> game.id,
                      "fullId" -> pov.fullId,
                      "color" -> pov.color.name,
                      "fen" -> Forsyth.exportBoard(game.toChess.board),
                      "lastMove" -> game.castleLastMoveTime.lastMoveString,
                      "secondsLeft" -> pov.remainingSeconds)
                  ))
              }
            }
          }
        }
      case _ => funit
    }
  }

  private def IfAway(pov: Pov)(f: => Funit): Funit = {
    import makeTimeout.short
    roundSocketHub ? Ask(pov.gameId, IsOnGame(pov.color)) mapTo manifest[Boolean] flatMap {
      case true  => funit
      case false => f
    }
  }

  private def opponentName(pov: Pov) = Namer playerString pov.opponent
}
