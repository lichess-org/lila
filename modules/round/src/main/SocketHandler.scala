package lila.round

import scala.concurrent.duration._
import scala.concurrent.Promise

import akka.actor._
import akka.pattern.ask
import chess.format.Uci
import play.api.libs.json.{ JsObject, Json }

import actorApi._, round._
import lila.common.ApiVersion
import lila.common.PimpedJson._
import lila.game.{ Pov, PovRef, GameRepo }
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.Berserk
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.Uid
import lila.user.User
import makeTimeout.short

private[round] final class SocketHandler(
    roundMap: ActorRef,
    socketHub: ActorRef,
    hub: lila.hub.Env,
    messenger: Messenger,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    bus: lila.common.Bus) {

  private def controller(
    gameId: String,
    socket: ActorRef,
    uid: Uid,
    ref: PovRef,
    member: Member,
    me: Option[User]): Handler.Controller = {

    def send(msg: Any) { roundMap ! Tell(gameId, msg) }

    def ping(o: JsObject) =
      o int "v" foreach { v => socket ! PingVersion(uid.value, v) }

    member.playerIdOption.fold[Handler.Controller](({
      case ("p", o)         => ping(o)
      case ("talk", o)      => o str "d" foreach { messenger.watcher(gameId, member, _) }
      case ("outoftime", _) => send(Outoftime)
    }: Handler.Controller) orElse evalCacheHandler(member, me) orElse lila.chat.Socket.in(
      chatId = s"$gameId/w",
      member = member,
      socket = socket,
      chat = messenger.chat
    )) { playerId =>
      ({
        case ("p", o) => ping(o)
        case ("move", o) => parseMove(o) foreach {
          case (move, blur, lag) =>
            val promise = Promise[Unit]
            promise.future onFailure {
              case _: Exception => socket ! Resync(uid.value)
            }
            send(HumanPlay(playerId, move, blur, lag.millis, promise.some))
            member push ackEvent
        }
        case ("drop", o) => parseDrop(o) foreach {
          case (drop, blur, lag) =>
            val promise = Promise[Unit]
            promise.future onFailure {
              case _: Exception => socket ! Resync(uid.value)
            }
            send(HumanPlay(playerId, drop, blur, lag.millis, promise.some))
            member push ackEvent
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
        case ("moretime", _)     => send(Moretime(playerId))
        case ("outoftime", _)    => send(Outoftime)
        case ("bye2", _)         => socket ! Bye(ref.color)
        case ("talk", o)         => o str "d" foreach { messenger.owner(gameId, member, _) }
        case ("hold", o) => for {
          d ← o obj "d"
          mean ← d int "mean"
          sd ← d int "sd"
        } send(HoldAlert(playerId, mean, sd, member.ip))
        case ("berserk", _) => member.userId foreach { userId =>
          hub.actor.tournamentApi ! Berserk(gameId, userId)
          member push ackEvent
        }
      }: Handler.Controller) orElse lila.chat.Socket.in(
        chatId = gameId,
        member = member,
        socket = socket,
        chat = messenger.chat)
    }
  }

  def watcher(
    gameId: String,
    colorName: String,
    uid: Uid,
    user: Option[User],
    ip: String,
    userTv: Option[String],
    apiVersion: ApiVersion): Fu[Option[JsSocketHandler]] =
    GameRepo.pov(gameId, colorName) flatMap {
      _ ?? { join(_, none, uid, user, ip, userTv = userTv, apiVersion) map some }
    }

  def player(
    pov: Pov,
    uid: Uid,
    user: Option[User],
    ip: String,
    apiVersion: ApiVersion): Fu[JsSocketHandler] =
    join(pov, Some(pov.playerId), uid, user, ip, userTv = none, apiVersion)

  private def join(
    pov: Pov,
    playerId: Option[String],
    uid: Uid,
    user: Option[User],
    ip: String,
    userTv: Option[String],
    apiVersion: ApiVersion): Fu[JsSocketHandler] = {
    val join = Join(
      uid = uid,
      user = user,
      color = pov.color,
      playerId = playerId,
      ip = ip,
      userTv = userTv,
      apiVersion = apiVersion)
    socketHub ? Get(pov.gameId) mapTo manifest[ActorRef] flatMap { socket =>
      Handler(hub, socket, uid, join) {
        case Connected(enum, member) =>
          // register to the TV channel when watching TV
          if (playerId.isEmpty && pov.game.isRecentTv)
            hub.channel.tvSelect ! lila.socket.Channel.Sub(member)
          // register to the tournament standing channel when playing a tournament game
          if (playerId.isDefined && pov.game.isTournament)
            hub.channel.tournamentStanding ! lila.socket.Channel.Sub(member)
          (controller(pov.gameId, socket, uid, pov.ref, member, user), enum, member)
      }
    }
  }

  private def parseMove(o: JsObject) = for {
    d ← o obj "d"
    move <- d str "u" flatMap Uci.Move.apply orElse parseOldMove(d)
    blur = d int "b" contains 1
  } yield (move, blur, parseLag(d))

  private def parseOldMove(d: JsObject) = for {
    orig ← d str "from"
    dest ← d str "to"
    prom = d str "promotion"
    move <- Uci.Move.fromStrings(orig, dest, prom)
  } yield move

  private def parseDrop(o: JsObject) = for {
    d ← o obj "d"
    role ← d str "role"
    pos ← d str "pos"
    drop <- Uci.Drop.fromStrings(role, pos)
    blur = d int "b" contains 1
  } yield (drop, blur, parseLag(d))

  private def parseLag(d: JsObject): Int =
    d.int("l") orElse d.int("lag") getOrElse 0

  private val ackEvent = Json.obj("t" -> "ack")
}
