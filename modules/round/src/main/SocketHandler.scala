package lila.round

import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.util.Try

import chess.format.Uci
import chess.{ Centis, MoveMetrics, Color }
import play.api.libs.json.{ JsObject, JsNumber, Json, Reads }

import actorApi._, round._
import lila.chat.Chat
import lila.common.{ IpAddress, ApiVersion, IsMobile }
import lila.game.{ Pov, PovRef, Game }
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.{ Berserk, RematchYes, RematchNo, Abort, Resign }
import lila.hub.actorApi.shutup.PublicSource
import lila.hub.DuctMap
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket
import lila.user.User
import makeTimeout.short

private[round] final class SocketHandler(
    roundMap: DuctMap[Round],
    socketMap: SocketMap,
    hub: lila.hub.Env,
    messenger: Messenger,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    selfReport: SelfReport,
    bus: lila.common.Bus,
    isRecentTv: Game.ID => Boolean
) {

  import SocketHandler._

  private def controller(
    gameId: Game.ID,
    chat: Option[Chat.Setup], // if using a non-game chat (tournament, simul, ...)
    socket: RoundSocket,
    uid: Socket.Uid,
    ref: PovRef,
    member: Member,
    ip: IpAddress,
    me: Option[User],
    mobile: IsMobile,
    onPing: () => Unit
  ): Handler.Controller = {

    def send(msg: Any): Unit = roundMap.tell(gameId, msg)

    def handlePing(o: JsObject) = {
      onPing()
      Handler.recordUserLagFromPing(member, o)
    }

    member.playerIdOption.fold[Handler.Controller](({
      case ("p", o) => handlePing(o)
      case ("talk", o) => o str "d" foreach { messenger.watcher(gameId, member, _) }
      case ("outoftime", _) => send(QuietFlag) // mobile app BC
      case ("flag", o) => clientFlag(o, none) foreach send
    }: Handler.Controller) orElse evalCacheHandler(uid, member, me) orElse lila.chat.Socket.in(
      chatId = Chat.Id(s"$gameId/w"),
      member = member,
      chat = messenger.chat,
      publicSource = PublicSource.Watcher(gameId).some
    )) { playerId =>
      ({
        case ("p", o) => handlePing(o)
        case ("move", o) => parseMove(o) foreach {
          case (move, blur, lag, ackId) =>
            val promise = Promise[Unit]
            promise.future onFailure {
              case _: Exception => socket ! Resync(uid)
            }
            send(HumanPlay(playerId, move, blur, lag, promise.some))
            member.push(ackMessage(ackId))
        }
        case ("drop", o) => parseDrop(o) foreach {
          case (drop, blur, lag, ackId) =>
            val promise = Promise[Unit]
            promise.future onFailure {
              case _: Exception => socket ! Resync(uid)
            }
            send(HumanPlay(playerId, drop, blur, lag, promise.some))
            member.push(ackMessage(ackId))
        }
        case ("rematch-yes", _) => send(RematchYes(playerId))
        case ("rematch-no", _) => send(RematchNo(playerId))
        case ("takeback-yes", _) => send(TakebackYes(playerId))
        case ("takeback-no", _) => send(TakebackNo(playerId))
        case ("draw-yes", _) => send(DrawYes(playerId))
        case ("draw-no", _) => send(DrawNo(playerId))
        case ("draw-claim", _) => send(DrawClaim(playerId))
        case ("resign", _) => send(Resign(playerId))
        case ("resign-force", _) => send(ResignForce(playerId))
        case ("draw-force", _) => send(DrawForce(playerId))
        case ("abort", _) => send(Abort(playerId))
        case ("moretime", _) => send(Moretime(playerId))
        case ("outoftime", _) => send(QuietFlag) // mobile app BC
        case ("flag", o) => clientFlag(o, playerId.some) foreach send
        case ("bye2", _) => socket ! Bye(ref.color)
        case ("talk", o) if chat.isEmpty => o str "d" foreach { messenger.owner(gameId, member, _) }
        case ("hold", o) => for {
          d ← o obj "d"
          mean ← d int "mean"
          sd ← d int "sd"
        } send(HoldAlert(playerId, mean, sd, ip))
        case ("berserk", o) => member.userId foreach { userId =>
          hub.tournamentApi ! Berserk(gameId, userId)
          member.push(ackMessage((o \ "d" \ "a").asOpt[AckId]))
        }
        case ("rep", o) => for {
          d ← o obj "d"
          name ← d str "n"
        } selfReport(member.userId, ip, s"$gameId$playerId", name)
      }: Handler.Controller) orElse lila.chat.Socket.in(
        chatId = chat.fold(Chat.Id(gameId))(_.id),
        publicSource = chat.map(_.publicSource),
        member = member,
        chat = messenger.chat
      )
    }
  }

  def watcher(
    pov: Pov,
    uid: Socket.Uid,
    user: Option[User],
    ip: IpAddress,
    userTv: Option[UserTv],
    version: Option[Socket.SocketVersion],
    apiVersion: ApiVersion,
    mobile: IsMobile
  ): Fu[JsSocketHandler] = join(pov, none, uid, user, ip, userTv, version, apiVersion, mobile)

  def player(
    pov: Pov,
    uid: Socket.Uid,
    user: Option[User],
    ip: IpAddress,
    version: Option[Socket.SocketVersion],
    apiVersion: ApiVersion,
    mobile: IsMobile
  ): Fu[JsSocketHandler] =
    join(pov, Some(pov.playerId), uid, user, ip, none, version, apiVersion, mobile)

  private def join(
    pov: Pov,
    playerId: Option[String],
    uid: Socket.Uid,
    user: Option[User],
    ip: IpAddress,
    userTv: Option[UserTv],
    version: Option[Socket.SocketVersion],
    apiVersion: ApiVersion,
    mobile: IsMobile
  ): Fu[JsSocketHandler] = {
    val socket = socketMap getOrMake pov.gameId
    socket.ask[Connected](promise => Join(
      uid = uid,
      user = user,
      color = pov.color,
      playerId = playerId,
      userTv = userTv,
      version = version,
      mobile = mobile,
      promise = promise
    )) map {
      case Connected(enum, member) =>
        // register to the TV channel when watching TV
        if (playerId.isEmpty && isRecentTv(pov.gameId)) bus.publish(
          lila.socket.Channel.Sub(member),
          'tvSelectChannel
        )
        // non-game chat, for tournament or simul games; only for players
        val chatSetup = playerId.isDefined ?? {
          pov.game.tournamentId.map(Chat.tournamentSetup) orElse pov.game.simulId.map(Chat.simulSetup)
        }
        val onPing: Handler.OnPing =
          if (member.owner) (_, _, _, _) => {
            Handler.defaultOnPing(socket, member, uid, apiVersion)
            if (member.owner) socket.playerDo(member.color, _.ping)
          }
          else Handler.defaultOnPing

        Handler.iteratee(
          hub,
          controller(pov.gameId, chatSetup, socket, uid, pov.ref, member, ip, user, mobile,
            () => onPing(socket, member, uid, apiVersion)),
          member,
          socket,
          uid,
          apiVersion,
          onPing = onPing
        ) -> enum
    }
  }

  private def parseMove(o: JsObject) = for {
    d ← o obj "d"
    move <- d str "u" flatMap Uci.Move.apply orElse parseOldMove(d)
    blur = d int "b" contains 1
    ackId = d.get[AckId]("a")
  } yield (move, blur, parseLag(d), ackId)

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
    ackId = d.get[AckId]("a")
  } yield (drop, blur, parseLag(d), ackId)

  private def parseLag(d: JsObject) = MoveMetrics(
    d.int("l") orElse d.int("lag") map Centis.ofMillis,
    d.str("s") flatMap { v => Try(Centis(Integer.parseInt(v, 36))).toOption }
  )

  private def clientFlag(o: JsObject, playerId: Option[String]) =
    o str "d" flatMap Color.apply map { ClientFlag(_, playerId) }

  private val ackEmpty = Json.obj("t" -> "ack")
  private def ackMessage(id: Option[AckId]) = id.fold(ackEmpty) { ackId =>
    ackEmpty + ("d" -> JsNumber(ackId.value))
  }
}

private object SocketHandler {
  case class AckId(value: Int)
  implicit val ackIdReads: Reads[AckId] = Reads.of[Int] map AckId.apply
}
