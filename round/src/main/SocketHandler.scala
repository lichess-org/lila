package lila.round

import akka.actor._
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsObject
import scalaz.{ Success, Failure }

import actorApi._
import lila.game.{ Pov, PovRef, GameRepo }
import lila.user.{ User, Context }
import chess.Color
import lila.socket._
import lila.socket.actorApi._
import lila.security.Flood
import lila.common.PimpedJson._
import makeTimeout.short

private[round] final class SocketHandler(
    hand: Hand,
    socketHub: ActorRef,
    messenger: Messenger,
    moveNotifier: MoveNotifier,
    flood: Flood,
    hijack: Hijack) {

  private def controller(
    socket: ActorRef,
    uid: String,
    povRef: PovRef)(member: Member): Handler.Controller =
    if (member.owner) {
      case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
      case ("talk", o) ⇒ for {
        txt ← o str "d"
        if member.canChat
        if flood.allowMessage(uid, txt)
      } messenger.playerMessage(povRef, txt) foreach { events ⇒
        socket ! Events(events)
      }
      case ("move", o) ⇒ parseMove(o) foreach {
        case (orig, dest, prom, blur, lag) ⇒ {
          socket ! Ack(uid)
          hand.play(povRef, orig, dest, prom, blur, lag) onSuccess {
            case Failure(fs) ⇒ {
              socket ! Resync(uid)
              logwarn(fs.shows)
            }
            case Success((events, fen, lastMove)) ⇒ {
              socketHub ! GameEvents(povRef.gameId, events)
              moveNotifier(povRef.gameId, fen, lastMove)
            }
          }
        }
      }
      case ("moretime", o) ⇒ hand moretime povRef foreach {
        _ foreach { events ⇒ socket ! Events(events) }
      }
      case ("outoftime", o) ⇒ hand outoftime povRef foreach {
        _ foreach { events ⇒ socket ! Events(events) }
      }
    }
    else {
      case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
      case ("talk", o) ⇒ for {
        txt ← o str "d"
        if member.canChat
        if flood.allowMessage(uid, txt)
      } messenger.watcherMessage(
        povRef.gameId,
        member.userId,
        txt) foreach { events ⇒ socket ! Events(events) }
    }

  def joinWatcher(
    gameId: String,
    colorName: String,
    version: Int,
    uid: String,
    ctx: Context): Fu[JsSocketHandler] =
    GameRepo.pov(gameId, colorName) flatMap {
      _ zmap { join(_, false, version, uid, "", ctx) }
    }

  def joinPlayer(
    fullId: String,
    version: Int,
    uid: String,
    token: String,
    ctx: Context): Fu[JsSocketHandler] =
    GameRepo.pov(fullId) flatMap {
      _ zmap { join(_, true, version, uid, token, ctx) }
    }

  private def join(
    pov: Pov,
    owner: Boolean,
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
      owner = owner && !hijack(pov, token, ctx))
    handler ← Handler(socket, uid, join)(controller(socket, uid, pov.ref))
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
