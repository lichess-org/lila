package lila.push

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import chess.format.Forsyth
import lila.challenge.Challenge
import lila.common.LightUser
import lila.game.{ Game, Pov, Namer }
import lila.user.User
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ MoveEvent, IsOnGame }
import lila.message.{ Thread, Post }

private final class PushApi(
    firebasePush: FirebasePush,
    oneSignalPush: OneSignalPush,
    webPush: WebPush,
    implicit val lightUser: LightUser.GetterSync,
    gameProxy: Game.ID => Fu[Option[Game]],
    scheduler: lila.common.Scheduler,
    system: ActorSystem
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
                "fullId" -> pov.fullId
              )
            )
          ))
        }
      }
    }.sequenceFu.void

  def move(move: MoveEvent): Funit = scheduler.after(2 seconds) {
    gameProxy(move.gameId) flatMap {
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
                  "userData" -> corresGameJson(pov, "gameMove")
                )
              ))
            }
          }
        }
      }
    }
  }

  def takebackOffer(gameId: Game.ID): Funit = scheduler.after(1 seconds) {
    gameProxy(gameId) flatMap {
      _.filter(_.playable).?? { game =>
        game.players.collectFirst {
          case p if p.isProposingTakeback => Pov(game, game opponent p)
        } ?? { pov => // the pov of the receiver
          pov.player.userId ?? { userId =>
            IfAway(pov) {
              pushToAll(userId, _.takeback, PushApi.Data(
                title = "Takeback offer",
                body = s"${opponentName(pov)} proposes a takeback",
                stacking = Stacking.GameTakebackOffer,
                payload = Json.obj(
                  "userId" -> userId,
                  "userData" -> corresGameJson(pov, "gameTakebackOffer")
                )
              ))
            }
          }
        }
      }
    }
  }

  def drawOffer(gameId: Game.ID): Funit = scheduler.after(1 seconds) {
    gameProxy(gameId) flatMap {
      _.filter(_.playable).?? { game =>
        game.players.collectFirst {
          case p if p.isOfferingDraw => Pov(game, game opponent p)
        } ?? { pov => // the pov of the receiver
          pov.player.userId ?? { userId =>
            IfAway(pov) {
              pushToAll(userId, _.takeback, PushApi.Data(
                title = "Draw offer",
                body = s"${opponentName(pov)} offers a draw",
                stacking = Stacking.GameDrawOffer,
                payload = Json.obj(
                  "userId" -> userId,
                  "userData" -> corresGameJson(pov, "gameDrawOffer")
                )
              ))
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
          "userData" -> corresGameJson(pov, "corresAlarm")
        )
      ))
    }

  private def corresGameJson(pov: Pov, typ: String) = Json.obj(
    "type" -> typ,
    "gameId" -> pov.gameId,
    "fullId" -> pov.fullId
  )

  def newMessage(t: Thread, p: Post): Funit =
    lightUser(t.visibleSenderOf(p)) ?? { sender =>
      lila.user.UserRepo.isKid(t receiverOf p) flatMap {
        case true => funit
        case _ => pushToAll(t receiverOf p, _.message, PushApi.Data(
          title = s"${sender.titleName}: ${t.name}",
          body = p.text take 140,
          stacking = Stacking.NewMessage,
          payload = Json.obj(
            "userId" -> t.receiverOf(p),
            "userData" -> Json.obj(
              "type" -> "newMessage",
              "threadId" -> t.id
            )
          )
        ))
      }
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
            "challengeId" -> c.id
          )
        )
      ))
    }

  private type MonitorType = lila.mon.push.send.type => (String => Unit)

  private def pushToAll(userId: User.ID, monitor: MonitorType, data: PushApi.Data): Funit =
    webPush(userId)(data) >> oneSignalPush(userId) {
      monitor(lila.mon.push.send)("onesignal")
      data
    } >> firebasePush(userId) {
      monitor(lila.mon.push.send)("firebase")
      data
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

  private def IfAway(pov: Pov)(f: => Funit): Funit =
    lila.common.Bus.ask[Boolean]('roundSocket) { p =>
      Tell(pov.gameId, IsOnGame(pov.color, p))
    }(system) flatMap {
      case true => funit
      case false => f
    }

  private def opponentName(pov: Pov) = Namer playerText pov.opponent

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
