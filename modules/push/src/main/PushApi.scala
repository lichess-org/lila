package lila.push

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._
import scala.concurrent.duration._

import chess.format.Forsyth
import lila.challenge.Challenge
import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov, Namer }
import lila.hub.actorApi.map.Ask
import lila.hub.actorApi.round.{ MoveEvent, IsOnGame }
import lila.message.{ Thread, Post }

private final class PushApi(
    googlePush: GooglePush,
    oneSignalPush: OneSignalPush,
    implicit val lightUser: LightUser.GetterSync,
    roundSocketHub: ActorSelection,
    scheduler: lila.common.Scheduler
) {

  def finish(game: Game): Funit =
    if (!game.isCorrespondence || game.hasAi) funit
    else game.userIds.map { userId =>
      Pov.ofUserId(game, userId) ?? { pov =>
        IfAway(pov) {
          pushToAll(userId, _.finish, PushApi.Data(
            title = pov.win match {
              case Some(true) => "You won!"
              case Some(false) => "You lost."
              case _ => "It's a draw."
            },
            body = s"Your game with ${opponentName(pov)} is over.",
            stacking = Stacking.GameFinish,
            payload = Json.obj(
              "userId" -> userId,
              "userData" -> Json.obj(
                "type" -> "gameFinish",
                "gameId" -> game.id,
                "fullId" -> pov.fullId,
                "color" -> pov.color.name,
                "fen" -> Forsyth.exportBoard(game.toChess.board),
                "lastMove" -> game.castleLastMoveTime.lastMoveString,
                "win" -> pov.win
              )
            )
          ))
        }
      }
    }.sequenceFu.void

  def move(move: MoveEvent): Funit = move.mobilePushable ?? {
    scheduler.after(2 seconds) {
      GameRepo game move.gameId flatMap {
        _.filter(_.playable) ?? { game =>
          val pov = Pov(game, game.player.color)
          game.player.userId ?? { userId =>
            IfAway(pov) {
              game.pgnMoves.lastOption ?? { sanMove =>
                pushToAll(userId, _.move, PushApi.Data(
                  title = "It's your turn!",
                  body = s"${opponentName(pov)} played $sanMove",
                  stacking = Stacking.GameMove,
                  payload = Json.obj(
                    "userId" -> userId,
                    "userData" -> Json.obj(
                      "type" -> "gameMove",
                      "gameId" -> game.id,
                      "fullId" -> pov.fullId,
                      "color" -> pov.color.name,
                      "fen" -> Forsyth.exportBoard(game.toChess.board),
                      "lastMove" -> game.castleLastMoveTime.lastMoveString,
                      "secondsLeft" -> pov.remainingSeconds
                    )
                  )
                ))
              }
            }
          }
        }
      }
    }
  }

  def corresAlarm(pov: Pov): Funit =
    pov.player.userId ?? { userId =>
      pushToAll(userId, _.corresAlarm, PushApi.Data(
        title = "Time is almost up!",
        body = s"You are about to lose on time against ${opponentName(pov)}",
        stacking = Stacking.GameMove,
        payload = Json.obj(
          "userId" -> userId,
          "userData" -> Json.obj(
            "type" -> "corresAlarm",
            "gameId" -> pov.gameId,
            "fullId" -> pov.fullId,
            "color" -> pov.color.name,
            "fen" -> Forsyth.exportBoard(pov.game.toChess.board),
            "lastMove" -> pov.game.castleLastMoveTime.lastMoveString,
            "secondsLeft" -> pov.remainingSeconds
          )
        )
      ))
    }

  def newMessage(t: Thread, p: Post): Funit =
    lightUser(t.visibleSenderOf(p)) ?? { sender =>
      pushToAll(t.receiverOf(p), _.message, PushApi.Data(
        title = s"${sender.titleName}: ${t.name}",
        body = p.text take 140,
        stacking = Stacking.NewMessage,
        payload = Json.obj(
          "userId" -> t.receiverOf(p),
          "userData" -> Json.obj(
            "type" -> "newMessage",
            "threadId" -> t.id,
            "sender" -> sender
          )
        )
      ))
    }

  def challengeCreate(c: Challenge): Funit = c.destUser ?? { dest =>
    c.challengerUser.ifFalse(c.hasClock) ?? { challenger =>
      lightUser(challenger.id) ?? { lightChallenger =>
        pushToAll(dest.id, _.challenge.create, PushApi.Data(
          title = s"${lightChallenger.titleName} (${challenger.rating.show}) challenges you!",
          body = describeChallenge(c),
          stacking = Stacking.ChallengeCreate,
          payload = Json.obj(
            "userId" -> dest.id,
            "userData" -> Json.obj(
              "type" -> "challengeCreate",
              "challengeId" -> c.id
            )
          )
        ))
      }
    }
  }

  def challengeAccept(c: Challenge, joinerId: Option[String]): Funit =
    c.challengerUser.ifTrue(c.finalColor.white && !c.hasClock) ?? { challenger =>
      val lightJoiner = joinerId flatMap lightUser
      pushToAll(challenger.id, _.challenge.accept, PushApi.Data(
        title = s"${lightJoiner.fold("Anonymous")(_.titleName)} accepts your challenge!",
        body = describeChallenge(c),
        stacking = Stacking.ChallengeAccept,
        payload = Json.obj(
          "userId" -> challenger.id,
          "userData" -> Json.obj(
            "type" -> "challengeAccept",
            "challengeId" -> c.id,
            "joiner" -> lightJoiner
          )
        )
      ))
    }

  private type MonitorType = lila.mon.push.send.type => (String => Unit)

  private def pushToAll(userId: String, monitor: MonitorType, data: PushApi.Data) = {
    oneSignalPush(userId) {
      monitor(lila.mon.push.send)("onesignal")
      data
    }
    googlePush(userId) {
      monitor(lila.mon.push.send)("android")
      data
    }
  }

  private def describeChallenge(c: Challenge) = {
    import lila.challenge.Challenge.TimeControl._
    List(
      c.mode.fold("Casual", "Rated"),
      c.timeControl match {
        case Unlimited => "Unlimited"
        case Correspondence(d) => s"$d days"
        case c: Clock => c.show
      },
      c.variant.name
    ) mkString " • "
  }

  private def IfAway(pov: Pov)(f: => Funit): Funit = {
    import makeTimeout.short
    roundSocketHub ? Ask(pov.gameId, IsOnGame(pov.color)) mapTo manifest[Boolean] flatMap {
      case true => funit
      case false => f
    }
  }

  private def opponentName(pov: Pov) = Namer playerString pov.opponent

  private implicit val lightUserWriter: OWrites[LightUser] = OWrites { u =>
    Json.obj(
      "id" -> u.id,
      "name" -> u.name,
      "title" -> u.title
    )
  }
}

private object PushApi {

  case class Data(
    title: String,
    body: String,
    stacking: Stacking,
    payload: JsObject
  )
}
