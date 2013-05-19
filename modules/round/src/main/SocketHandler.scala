package lila.round

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.JsObject

import actorApi._, round._
import lila.hub.actorApi.Tell
import lila.game.{ Game, Pov, PovRef, PlayerRef, GameRepo }
import lila.user.{ User, Context }
import chess.Color
import lila.socket.Handler
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.security.Flood
import lila.common.PimpedJson._
import makeTimeout.short

private[round] final class SocketHandler(
    roundMap: ActorRef,
    socketHub: ActorRef,
    hub: lila.hub.Env,
    messenger: Messenger,
    flood: Flood) {

  private def controller(
    gameId: String,
    socket: ActorRef,
    uid: String,
    ref: PovRef,
    member: Member): Handler.Controller = {

    member.playerIdOption.fold[Handler.Controller]({
      case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
      case ("talk", o) ⇒ for {
        txt ← o str "d"
        if flood.allowMessage(uid, txt)
      } messenger.watcherMessage(
        ref.gameId,
        member.userId,
        txt) pipeTo socket
    }) { playerId ⇒
      {
        case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
        case ("talk", o) ⇒ for {
          txt ← o str "d"
          if flood.allowMessage(uid, txt)
        } messenger.playerMessage(ref, txt) pipeTo socket
        case ("rematch-yes", _)  ⇒ roundMap ! Tell(gameId, RematchYes(playerId))
        case ("rematch-no", _)   ⇒ roundMap ! Tell(gameId, RematchNo(playerId))
        case ("takeback-yes", _) ⇒ roundMap ! Tell(gameId, TakebackYes(playerId))
        case ("takeback-no", _)  ⇒ roundMap ! Tell(gameId, TakebackNo(playerId))
        case ("draw-yes", _)     ⇒ roundMap ! Tell(gameId, DrawYes(playerId))
        case ("draw-no", _)      ⇒ roundMap ! Tell(gameId, DrawNo(playerId))
        case ("draw-claim", _)   ⇒ roundMap ! Tell(gameId, DrawClaim(playerId))
        case ("resign", _)       ⇒ roundMap ! Tell(gameId, Resign(playerId))
        case ("resign-force", _) ⇒ roundMap ! Tell(gameId, ResignForce(playerId))
        case ("abort", _)        ⇒ roundMap ! Tell(gameId, Abort(playerId))
        case ("move", o) ⇒ parseMove(o) foreach {
          case (orig, dest, prom, blur, lag) ⇒ {
            socket ! Ack(uid)
            roundMap ! Tell(
              gameId,
              HumanPlay(playerId, orig, dest, prom, blur, lag, _ ⇒ socket ! Resync(uid))
            )
          }
        }
        case ("moretime", o)  ⇒ roundMap ! Tell(gameId, Moretime(playerId))
        case ("outoftime", o) ⇒ roundMap ! Tell(gameId, Outoftime)
      }
    }
  }

  def watcher(
    gameId: String,
    colorName: String,
    version: Int,
    uid: String,
    ctx: Context): Fu[JsSocketHandler] =
    GameRepo.pov(gameId, colorName) flatMap {
      _ ?? { join(_, none, version, uid, ctx) }
    }

  def player(
    fullId: String,
    version: Int,
    uid: String,
    ctx: Context): Fu[JsSocketHandler] =
    GameRepo.pov(fullId) flatMap {
      _ ?? { join(_, Some(Game takePlayerId fullId), version, uid, ctx) }
    }

  private def join(
    pov: Pov,
    playerId: Option[String],
    version: Int,
    uid: String,
    ctx: Context): Fu[JsSocketHandler] = for {
    socket ← socketHub ? GetSocket(pov.gameId) mapTo manifest[ActorRef]
    join = Join(
      uid = uid,
      user = ctx.me,
      version = version,
      color = pov.color,
      playerId = playerId)
    handler ← Handler(hub, socket, uid, join, ctx.userId) {
      case Connected(enum, member) ⇒
        controller(pov.gameId, socket, uid, pov.ref, member) -> enum
    }
  } yield handler

  private def parseMove(o: JsObject) = for {
    d ← o obj "d"
    orig ← d str "from"
    dest ← d str "to"
    prom = d str "promotion"
    blur = (d int "b") == Some(1)
    lag = d int "lag"
  } yield (orig, dest, prom, blur, ~lag)
}
