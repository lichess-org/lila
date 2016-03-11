package lila.round

import scala.concurrent.duration._
import scala.concurrent.Promise

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import chess.format.Uci
import play.api.libs.json.{ JsObject, Json }

import actorApi._, round._
import lila.common.PimpedJson._
import lila.game.{ Game, Pov, PovRef, PlayerRef, GameRepo }
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.Berserk
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

    def send(msg: Any) { roundMap ! Tell(gameId, msg) }

    member.playerIdOption.fold[Handler.Controller]({
      case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
      case ("talk", o) => o str "d" foreach { text =>
        messenger.watcher(gameId, member, text, socket)
      }
      case ("outoftime", _) => send(Outoftime)
    }) { playerId =>
      {
        case ("p", o)            => o int "v" foreach { v => socket ! PingVersion(uid, v) }
        case ("move", o) => parseMove(o) foreach {
          case (move, blur, lag) =>
            member push ackEvent
            val promise = Promise[Unit]
            promise.future onFailure {
              case _: Exception => socket ! Resync(uid)
            }
            send(HumanPlay(
              playerId, move, blur, lag.millis, promise.some
            ))
        }
        case ("drop", o) => parseDrop(o) foreach {
          case (drop, blur, lag) =>
            member push ackEvent
            val promise = Promise[Unit]
            promise.future onFailure {
              case _: Exception => socket ! Resync(uid)
            }
            send(HumanPlay(
              playerId, drop, blur, lag.millis, promise.some
            ))
        }
        case ("rematch-yes", _)  => send(RematchYes(playerId))
        case ("rematch-no", _)   => send(RematchNo(playerId))
        case ("takeback-yes", _) => send(TakebackYes(playerId))
        case ("takeback-no", _)  => send(TakebackNo(playerId))
        case ("draw-yes", _)     => send(DrawYes(playerId))
        case ("draw-no", _)      => send(DrawNo(playerId))
        case ("draw-claim", _)   => send(DrawClaim(playerId))
        case ("resign", _)       => send(Resign(playerId))
        case ("resign-force", _) => send(ResignForce(playerId))
        case ("draw-force", _)   => send(DrawForce(playerId))
        case ("abort", _)        => send(Abort(playerId))
        case ("moretime", _)  => send(Moretime(playerId))
        case ("outoftime", _) => send(Outoftime)
        case ("bye", _)       => socket ! Bye(ref.color)
        case ("talk", o) => o str "d" foreach { text =>
          messenger.owner(gameId, member, text, socket)
        }
        case ("hold", o) => for {
          d ← o obj "d"
          mean ← d int "mean"
          sd ← d int "sd"
        } send(HoldAlert(playerId, mean, sd, member.ip))
        case ("berserk", _) => member.userId foreach { userId =>
          hub.actor.tournamentOrganizer ! Berserk(gameId, userId)
        }
      }
    }
  }

  def watcher(
    gameId: String,
    colorName: String,
    uid: String,
    user: Option[User],
    ip: String,
    userTv: Option[String]): Fu[Option[JsSocketHandler]] =
    GameRepo.pov(gameId, colorName) flatMap {
      _ ?? { join(_, none, uid, "", user, ip, userTv = userTv) map some }
    }

  def player(
    pov: Pov,
    uid: String,
    token: String,
    user: Option[User],
    ip: String): Fu[JsSocketHandler] =
    join(pov, Some(pov.playerId), uid, token, user, ip, userTv = none)

  private def join(
    pov: Pov,
    playerId: Option[String],
    uid: String,
    token: String,
    user: Option[User],
    ip: String,
    userTv: Option[String]): Fu[JsSocketHandler] = {
    val join = Join(
      uid = uid,
      user = user,
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
    move <- Uci.Move.fromStrings(orig, dest, prom)
    blur = (d int "b") == Some(1)
    lag = d int "lag"
  } yield (move, blur, ~lag)

  private def parseDrop(o: JsObject) = for {
    d ← o obj "d"
    role ← d str "role"
    pos ← d str "pos"
    drop <- Uci.Drop.fromStrings(role, pos)
    blur = (d int "b") == Some(1)
    lag = d int "lag"
  } yield (drop, blur, ~lag)

  private val ackEvent = Json.obj("t" -> "ack")
}
