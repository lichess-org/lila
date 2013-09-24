package lila.round

import actorApi._, round._
import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import makeTimeout.short
import play.api.libs.json.{ JsObject, Json }

import lila.common.PimpedJson._
import lila.game.{ Game, Pov, PovRef, PlayerRef, GameRepo }
import lila.hub.actorApi.map._
import lila.security.Flood
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.socket.Handler
import lila.user.{ User, Context }

private[round] final class SocketHandler(
    roundMap: ActorRef,
    socketHub: ActorRef,
    hub: lila.hub.Env,
    messenger: Messenger,
    flood: Flood,
    hijack: Hijack) {

  private def controller(
    gameId: String,
    socket: ActorRef,
    uid: String,
    ref: PovRef,
    member: Member): Handler.Controller = {

    def round(msg: Any) { roundMap ! Tell(gameId, msg) }

    def watcherTalk(roomId: String, o: JsObject) {
      (o str "d") filter {
        flood.allowMessage(uid, _)
      } foreach { txt ⇒
        messenger.watcherMessage(roomId, member.userId, txt, member.troll) pipeTo socket
      }
    }

    member.playerIdOption.fold[Handler.Controller]({
      case ("p", o)       ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
      case ("talk", o)    ⇒ watcherTalk(ref.gameId, o)
      case ("talk-tv", o) ⇒ if (member.isAuth) watcherTalk("tv", o)
    }) { playerId ⇒
      {
        case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
        case ("talk", o) ⇒ for {
          txt ← o str "d"
          if flood.allowMessage(uid, txt)
        } messenger.playerMessage(ref, txt, member.troll) pipeTo socket
        case ("rematch-yes", _)  ⇒ round(RematchYes(playerId))
        case ("rematch-no", _)   ⇒ round(RematchNo(playerId))
        case ("takeback-yes", _) ⇒ round(TakebackYes(playerId))
        case ("takeback-no", _)  ⇒ round(TakebackNo(playerId))
        case ("draw-yes", _)     ⇒ round(DrawYes(playerId))
        case ("draw-no", _)      ⇒ round(DrawNo(playerId))
        case ("draw-claim", _)   ⇒ round(DrawClaim(playerId))
        case ("resign", _)       ⇒ round(Resign(playerId))
        case ("resign-force", _) ⇒ round(ResignForce(playerId))
        case ("abort", _)        ⇒ round(Abort(playerId))
        case ("move", o) ⇒ parseMove(o) foreach {
          case (orig, dest, prom, blur, lag) ⇒ {
            socket ! Ack(uid)
            round(HumanPlay(
              playerId, orig, dest, prom, blur, lag, _ ⇒ socket ! Resync(uid)
            ))
          }
        }
        case ("moretime", _)  ⇒ round(Moretime(playerId))
        case ("outoftime", _) ⇒ round(Outoftime)
        case ("bye", _)       ⇒ socket ! Bye(ref.color)
        case ("toggle-chat", o) ⇒
          messenger.toggleChat(ref, ~(o boolean "d")) pipeTo socket
        case ("challenge", o) ⇒ ((o str "d") |@| member.userId).tupled foreach {
          case (to, from) ⇒ hub.actor.challenger ! lila.hub.actorApi.setup.RemindChallenge(gameId, from, to)
        }
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
    ctx: Context): Fu[JsSocketHandler] = {
    val join = Join(uid = uid, user = ctx.me, version = version, color = pov.color,
      playerId = playerId filterNot (_ ⇒ hijack(pov, token, ctx)))
    socketHub ? Get(pov.gameId) mapTo manifest[ActorRef] flatMap { socket ⇒
      Handler(hub, socket, uid, join, ctx.userId) {
        case Connected(enum, member) ⇒
          controller(pov.gameId, socket, uid, pov.ref, member) -> enum
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
}
