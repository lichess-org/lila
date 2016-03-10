package lila.push

import akka.actor._
import akka.pattern.ask
import chess.format.Forsyth
import lila.challenge.Challenge
import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov, Namer }
import lila.hub.actorApi.map.Ask
import lila.hub.actorApi.round.{ MoveEvent, IsOnGame }
import lila.user.User

import play.api.libs.json._
import kamon.Kamon

private final class PushApi(
    googlePush: GooglePush,
    implicit val lightUser: String => Option[LightUser],
    isOnline: User.ID => Boolean,
    roundSocketHub: ActorSelection) {

  def finish(game: Game): Funit =
    if (!game.isCorrespondence || game.hasAi) funit
    else game.userIds.map { userId =>
      Pov.ofUserId(game, userId) ?? { pov =>
        IfAway(pov) {
          googlePush(userId) {
            Kamon.metrics.counter(s"push.finish").increment()
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
                  "type" -> "gameFinish",
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

  def move(move: MoveEvent): Funit = move.mobilePushable ?? {
    GameRepo game move.gameId flatMap {
      _ ?? { game =>
        val pov = Pov(game, !move.color)
        game.player(!move.color).userId ?? { userId =>
          game.pgnMoves.lastOption ?? { sanMove =>
            IfAway(pov) {
              googlePush(userId) {
                Kamon.metrics.counter(s"push.move").increment()
                GooglePush.Data(
                  title = "It's your turn!",
                  body = s"${opponentName(pov)} played $sanMove",
                  payload = Json.obj(
                    "userId" -> userId,
                    "userData" -> Json.obj(
                      "type" -> "gameMove",
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
      }
    }
  }

  def challengeCreate(c: Challenge): Funit = c.destUser.filterNot(u => isOnline(u.id)) ?? { dest =>
    c.challengerUser ?? { challenger =>
      lightUser(challenger.id) ?? { lightChallenger =>
        googlePush(dest.id) {
          Kamon.metrics.counter(s"push.challenge.create").increment()
          GooglePush.Data(
            title = s"${lightChallenger.titleName} (${challenger.rating.show}) challenges you!",
            body = describeChallenge(c),
            payload = Json.obj(
              "userId" -> dest.id,
              "userData" -> Json.obj(
                "type" -> "challengeCreate",
                "challengeId" -> c.id))
          )
        }
      }
    }
  }

  def challengeAccept(c: Challenge, joinerId: Option[String]): Funit =
    c.challengerUser.ifTrue(c.finalColor.white).filterNot(u => isOnline(u.id)) ?? { challenger =>
      val lightJoiner = joinerId flatMap lightUser
      googlePush(challenger.id) {
        Kamon.metrics.counter(s"push.challenge.accept").increment()
        GooglePush.Data(
          title = s"${lightJoiner.fold("Anonymous")(_.titleName)} accepts your challenge!",
          body = describeChallenge(c),
          payload = Json.obj(
            "userId" -> challenger.id,
            "userData" -> Json.obj(
              "type" -> "challengeAccept",
              "challengeId" -> c.id))
        )
      }
    }

  private def describeChallenge(c: Challenge) = {
    import lila.challenge.Challenge.TimeControl._
    List(
      c.mode.fold("Casual", "Rated"),
      c.timeControl match {
        case Unlimited         => "Unlimited"
        case Correspondence(d) => s"$d days"
        case c: Clock          => c.show
      },
      c.variant.name
    ) mkString " • "
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
