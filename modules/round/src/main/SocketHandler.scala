package lila.round

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.JsObject

import actorApi._, round._
import lila.hub.actorApi.map._
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
    messenger: Messenger,
    flood: Flood,
    hijack: Hijack) {

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
        case ("rematch-yes", o) ⇒ roundMap ! Tell(gameId, RematchYes(playerId))
        case ("rematch-no", o)  ⇒ roundMap ! Tell(gameId, RematchNo(playerId))
        case ("move", o) ⇒ parseMove(o) foreach {
          case (orig, dest, prom, blur, lag) ⇒ {
            socket ! Ack(uid)
            roundMap ! Tell(
              gameId,
              Play(playerId, orig, dest, prom, blur, lag),
              _ ⇒ socket ! Resync(uid)
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
      _ ?? { join(_, none, version, uid, "", ctx) }
    }

  def player(
    fullId: String,
    version: Int,
    uid: String,
    token: String,
    ctx: Context): Fu[JsSocketHandler] =
    GameRepo.pov(fullId) flatMap {
      _ ?? { join(_, Some(Game takePlayerId fullId), version, uid, token, ctx) }
    }

  private def join(
    pov: Pov,
    playerId: Option[String],
    version: Int,
    uid: String,
    token: String,
    ctx: Context): Fu[JsSocketHandler] = for {
    socket ← socketHub ? GetSocket(pov.gameId) mapTo manifest[ActorRef]
    join = Join(
      uid = uid,
      user = ctx.me,
      version = version,
      color = pov.color,
      playerId = playerId filterNot (_ ⇒ hijack(pov, token, ctx)))
    handler ← Handler(socket, uid, join) {
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
