package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import play.api.libs.json.{ JsObject, Json }

import actorApi._, round._
import lila.common.PimpedJson._
import lila.game.{ Game, Pov, PovRef, PlayerRef, GameRepo }
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[round] final class SocketHandler(
    roundMap: ActorRef,
    socketHub: ActorRef,
    hub: lila.hub.Env,
    messenger: Messenger,
    bus: lila.common.Bus) {

  private def controller(
    gameId: String,
    socket: ActorRef,
    uid: String,
    ref: PovRef,
    member: Member): Handler.Controller = {

    def round(msg: Any) { roundMap ! Tell(gameId, msg) }

    member.playerIdOption.fold[Handler.Controller]({
      case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
      case ("startWatching", o) => o str "d" foreach { ids =>
        hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
      }
      case ("talk", o) => o str "d" foreach { text =>
        messenger.watcher(gameId, member, text, socket)
      }
      case ("outoftime", _) => round(Outoftime)
    }) { playerId =>
      {
        case ("p", o)            => o int "v" foreach { v => socket ! PingVersion(uid, v) }
        case ("rematch-yes", _)  => round(RematchYes(playerId))
        case ("rematch-no", _)   => round(RematchNo(playerId))
        case ("takeback-yes", _) => round(TakebackYes(playerId))
        case ("takeback-no", _)  => round(TakebackNo(playerId))
        case ("draw-yes", _)     => round(DrawYes(playerId))
        case ("draw-no", _)      => round(DrawNo(playerId))
        case ("draw-claim", _)   => round(DrawClaim(playerId))
        case ("resign", _)       => round(Resign(playerId))
        case ("resign-force", _) => round(ResignForce(playerId))
        case ("draw-force", _)   => round(DrawForce(playerId))
        case ("abort", _)        => round(Abort(playerId))
        case ("move", o) => parseMove(o) foreach {
          case (orig, dest, prom, blur, lag) =>
            member push ackEvent
            round(HumanPlay(
              playerId, member.ip, orig, dest, prom, blur, lag.millis, _ => socket ! Resync(uid)
            ))
        }
        case ("moretime", _)  => round(Moretime(playerId))
        case ("outoftime", _) => round(Outoftime)
        case ("bye", _)       => socket ! Bye(ref.color)
        case ("challenge", o) => ((o str "d") |@| member.userId) apply {
          case (to, from) => hub.actor.challenger ! lila.hub.actorApi.setup.RemindChallenge(gameId, from, to)
        }
        case ("startWatching", o) => o str "d" foreach { ids =>
          hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
        }
        case ("talk", o) => o str "d" foreach { text =>
          messenger.owner(gameId, member, text, socket)
        }
        case ("hold", o) => for {
          d ← o obj "d"
          mean ← d int "mean"
          sd ← d int "sd"
        } round(HoldAlert(playerId, mean, sd))
      }
    }
  }

  def watcher(
    gameId: String,
    colorName: String,
    version: Int,
    uid: String,
    user: Option[User],
    ip: String,
    userTv: Option[String]): Fu[Option[JsSocketHandler]] =
    GameRepo.pov(gameId, colorName) flatMap {
      _ ?? { join(_, none, version, uid, "", user, ip, userTv = userTv) map some }
    }

  def player(
    pov: Pov,
    version: Int,
    uid: String,
    token: String,
    user: Option[User],
    ip: String): Fu[JsSocketHandler] =
    join(pov, Some(pov.playerId), version, uid, token, user, ip, userTv = none)

  private def join(
    pov: Pov,
    playerId: Option[String],
    version: Int,
    uid: String,
    token: String,
    user: Option[User],
    ip: String,
    userTv: Option[String]): Fu[JsSocketHandler] = {
    val join = Join(
      uid = uid,
      user = user,
      version = version,
      color = pov.color,
      playerId = playerId,
      ip = ip,
      userTv = userTv)
    socketHub ? Get(pov.gameId) mapTo manifest[ActorRef] flatMap { socket =>
      Handler(hub, socket, uid, join, user map (_.id)) {
        case Connected(enum, member) =>
          (controller(pov.gameId, socket, uid, pov.ref, member), enum, member)
      }
    }
  }

  private def parseMove(o: JsObject) = for {
    d ← o obj "d"
    orig ← d str "from"
    dest ← d str "to"
    prom = d str "promotion"
    blur = (d int "b") == Some(1)
    lag = d int "lag"
  } yield (orig, dest, prom, blur, ~lag)

  private val ackEvent = Json.obj("t" -> "ack")
}
