package lila.push

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.challenge.Challenge
import lila.common.{ Future, LightUser }
import lila.game.{ Game, Namer, Pov }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.{ IsOnGame, MoveEvent }
import lila.user.User

final private class PushApi(
    firebasePush: FirebasePush,
    webPush: WebPush,
    userRepo: lila.user.UserRepo,
    implicit val lightUser: LightUser.Getter,
    proxyRepo: lila.round.GameProxyRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  def finish(game: Game): Funit =
    if (!game.isCorrespondence || game.hasAi) funit
    else
      game.userIds
        .map { userId =>
          Pov.ofUserId(game, userId) ?? { pov =>
            IfAway(pov) {
              asyncOpponentName(pov) flatMap { opponent =>
                pushToAll(
                  userId,
                  _.finish,
                  PushApi.Data(
                    title = pov.win match {
                      case Some(true)  => "You won!"
                      case Some(false) => "You lost."
                      case _           => "It's a draw."
                    },
                    body = s"Your game with $opponent is over.",
                    stacking = Stacking.GameFinish,
                    payload = Json.obj(
                      "userId" -> userId,
                      "userData" -> Json.obj(
                        "type"   -> "gameFinish",
                        "gameId" -> game.id,
                        "fullId" -> pov.fullId
                      )
                    )
                  )
                )
              }
            }
          }
        }
        .sequenceFu
        .void

  def move(move: MoveEvent): Funit =
    Future.delay(2 seconds) {
      proxyRepo.game(move.gameId) flatMap {
        _.filter(_.playable) ?? { game =>
          val pov = Pov(game, game.player.color)
          game.player.userId ?? { userId =>
            IfAway(pov) {
              asyncOpponentName(pov) flatMap { opponent =>
                game.pgnMoves.lastOption ?? { sanMove =>
                  pushToAll(
                    userId,
                    _.move,
                    PushApi.Data(
                      title = "It's your turn!",
                      body = s"$opponent played $sanMove",
                      stacking = Stacking.GameMove,
                      payload = Json.obj(
                        "userId"   -> userId,
                        "userData" -> corresGameJson(pov, "gameMove")
                      )
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

  def takebackOffer(gameId: Game.ID): Funit =
    Future.delay(1 seconds) {
      proxyRepo.game(gameId) flatMap {
        _.filter(_.playable).?? { game =>
          game.players.collectFirst {
            case p if p.isProposingTakeback => Pov(game, game opponent p)
          } ?? { pov => // the pov of the receiver
            pov.player.userId ?? { userId =>
              IfAway(pov) {
                asyncOpponentName(pov) flatMap { opponent =>
                  pushToAll(
                    userId,
                    _.takeback,
                    PushApi.Data(
                      title = "Takeback offer",
                      body = s"$opponent proposes a takeback",
                      stacking = Stacking.GameTakebackOffer,
                      payload = Json.obj(
                        "userId"   -> userId,
                        "userData" -> corresGameJson(pov, "gameTakebackOffer")
                      )
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

  def drawOffer(gameId: Game.ID): Funit =
    Future.delay(1 seconds) {
      proxyRepo.game(gameId) flatMap {
        _.filter(_.playable).?? { game =>
          game.players.collectFirst {
            case p if p.isOfferingDraw => Pov(game, game opponent p)
          } ?? { pov => // the pov of the receiver
            pov.player.userId ?? { userId =>
              IfAway(pov) {
                asyncOpponentName(pov) flatMap { opponent =>
                  pushToAll(
                    userId,
                    _.takeback,
                    PushApi.Data(
                      title = "Draw offer",
                      body = s"$opponent offers a draw",
                      stacking = Stacking.GameDrawOffer,
                      payload = Json.obj(
                        "userId"   -> userId,
                        "userData" -> corresGameJson(pov, "gameDrawOffer")
                      )
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

  def corresAlarm(pov: Pov): Funit =
    pov.player.userId ?? { userId =>
      asyncOpponentName(pov) flatMap { opponent =>
        pushToAll(
          userId,
          _.corresAlarm,
          PushApi.Data(
            title = "Time is almost up!",
            body = s"You are about to lose on time against $opponent",
            stacking = Stacking.GameMove,
            payload = Json.obj(
              "userId"   -> userId,
              "userData" -> corresGameJson(pov, "corresAlarm")
            )
          )
        )
      }
    }

  private def corresGameJson(pov: Pov, typ: String) =
    Json.obj(
      "type"   -> typ,
      "gameId" -> pov.gameId,
      "fullId" -> pov.fullId
    )

  def newMsg(t: lila.msg.MsgThread): Funit =
    lightUser(t.lastMsg.user) flatMap {
      _ ?? { sender =>
        userRepo.isKid(t other sender) flatMap {
          !_ ?? {
            pushToAll(
              t other sender,
              _.message,
              PushApi.Data(
                title = sender.titleName,
                body = t.lastMsg.text take 140,
                stacking = Stacking.NewMessage,
                payload = Json.obj(
                  "userId" -> t.other(sender),
                  "userData" -> Json.obj(
                    "type"     -> "newMessage",
                    "threadId" -> sender.id
                  )
                )
              )
            )
          }
        }
      }
    }

  def challengeCreate(c: Challenge): Funit =
    c.destUser ?? { dest =>
      c.challengerUser.ifFalse(c.hasClock) ?? { challenger =>
        lightUser(challenger.id) flatMap {
          _ ?? { lightChallenger =>
            pushToAll(
              dest.id,
              _.challenge.create,
              PushApi.Data(
                title = s"${lightChallenger.titleName} (${challenger.rating.show}) challenges you!",
                body = describeChallenge(c),
                stacking = Stacking.ChallengeCreate,
                payload = Json.obj(
                  "userId" -> dest.id,
                  "userData" -> Json.obj(
                    "type"        -> "challengeCreate",
                    "challengeId" -> c.id
                  )
                )
              )
            )
          }
        }
      }
    }

  def challengeAccept(c: Challenge, joinerId: Option[String]): Funit =
    c.challengerUser.ifTrue(c.finalColor.white && !c.hasClock) ?? { challenger =>
      joinerId ?? lightUser flatMap { lightJoiner =>
        pushToAll(
          challenger.id,
          _.challenge.accept,
          PushApi.Data(
            title = s"${lightJoiner.fold("Anonymous")(_.titleName)} accepts your challenge!",
            body = describeChallenge(c),
            stacking = Stacking.ChallengeAccept,
            payload = Json.obj(
              "userId" -> challenger.id,
              "userData" -> Json.obj(
                "type"        -> "challengeAccept",
                "challengeId" -> c.id
              )
            )
          )
        )
      }
    }

  private type MonitorType = lila.mon.push.send.type => ((String, Boolean) => Unit)

  private def pushToAll(userId: User.ID, monitor: MonitorType, data: PushApi.Data): Funit =
    webPush(userId, data).addEffects { res =>
      monitor(lila.mon.push.send)("web", res.isSuccess)
    } zip
      firebasePush(userId, data).addEffects { res =>
        monitor(lila.mon.push.send)("firebase", res.isSuccess)
      } void

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

  private def IfAway(pov: Pov)(f: => Funit): Funit =
    lila.common.Bus.ask[Boolean]("roundSocket") { p =>
      Tell(pov.gameId, IsOnGame(pov.color, p))
    } flatMap {
      case true  => funit
      case false => f
    }

  private def asyncOpponentName(pov: Pov): Fu[String] = Namer playerText pov.opponent
}

private object PushApi {

  case class Data(
      title: String,
      body: String,
      stacking: Stacking,
      payload: JsObject
  )
}
