package lila.lobby

import scala.concurrent.duration._
import play.api.libs.json._

import actorApi._
import lila.common.{ Tellable, ApiVersion }
import lila.pool.{ PoolApi, PoolConfig }
import lila.rating.RatingRange
import lila.socket.{ Socket, Handler }
import lila.user.User
import ornicar.scalalib.Zero

private[lobby] final class SocketHandler(
    hub: lila.hub.Env,
    lobby: LobbyTrouper,
    socket: LobbySocket,
    poolApi: PoolApi,
    blocking: String => Fu[Set[String]]
) extends Tellable.PartialReceive with Tellable.HashCode {

  private var pong = Socket.initialPong

  protected val receive: Tellable.Receive = {
    case lila.socket.actorApi.NbMembers(nb) => pong = pong + ("d" -> JsNumber(nb))
    case lila.hub.actorApi.round.NbRounds(nb) => pong = pong + ("r" -> JsNumber(nb))
  }

  private val HookPoolLimitPerMember = new lila.memo.RateLimit[String](
    credits = 25,
    duration = 1 minute,
    name = "lobby hook/pool per member",
    key = "lobby.hook_pool.member"
  )

  private def HookPoolLimit[A: Zero](member: Member, cost: Int, msg: => String)(op: => A) =
    HookPoolLimitPerMember(
      k = member.uid.value,
      cost = cost,
      msg = s"$msg mobile=${member.mobile}"
    )(op)

  private def controller(socket: LobbySocket, member: Member, isBot: Boolean): Handler.Controller = {
    case ("join", o) if !isBot => HookPoolLimit(member, cost = 5, msg = s"join $o") {
      o str "d" foreach { id =>
        lobby ! BiteHook(id, member.uid, member.user)
      }
    }
    case ("cancel", _) => HookPoolLimit(member, cost = 1, msg = "cancel") {
      lobby ! CancelHook(member.uid)
    }
    case ("joinSeek", o) if !isBot => HookPoolLimit(member, cost = 5, msg = s"joinSeek $o") {
      for {
        id <- o str "d"
        user <- member.user
      } lobby ! BiteSeek(id, user)
    }
    case ("cancelSeek", o) => HookPoolLimit(member, cost = 1, msg = s"cancelSeek $o") {
      for {
        id <- o str "d"
        user <- member.user
      } lobby ! CancelSeek(id, user)
    }
    case ("idle", o) => socket ! SetIdle(member.uid, ~(o boolean "d"))
    // entering a pool
    case ("poolIn", o) if !isBot => HookPoolLimit(member, cost = 1, msg = s"poolIn $o") {
      for {
        user <- member.user
        d <- o obj "d"
        id <- d str "id"
        ratingRange = d str "range" flatMap RatingRange.apply
        blocking = d str "blocking"
      } {
        lobby ! CancelHook(member.uid) // in case there's one...
        poolApi.join(
          PoolConfig.Id(id),
          PoolApi.Joiner(
            userId = user.id,
            uid = member.uid,
            ratingMap = user.perfMap.mapValues(_.rating),
            ratingRange = ratingRange,
            lame = user.lame,
            blocking = user.blocking ++ blocking
          )
        )
      }
    }
    // leaving a pool
    case ("poolOut", o) => HookPoolLimit(member, cost = 1, msg = s"poolOut $o") {
      for {
        id <- o str "d"
        user <- member.user
      } poolApi.leave(PoolConfig.Id(id), user.id)
    }
    // entering the hooks view
    case ("hookIn", _) => HookPoolLimit(member, cost = 2, msg = "hookIn") {
      lobby ! HookSub(member, true)
    }
    // leaving the hooks view
    case ("hookOut", _) => socket ! HookSub(member, false)
  }

  def apply(
    uid: Socket.Uid,
    user: Option[User],
    mobile: Boolean,
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] =
    (user ?? (u => blocking(u.id))) flatMap { blockedUserIds =>
      socket.ask[Connected](Join(uid, user, blockedUserIds, mobile, _)) map {
        case Connected(enum, member) => Handler.iteratee(
          hub,
          controller(socket, member, user.exists(_.isBot)),
          member,
          socket,
          uid,
          apiVersion,
          onPing = (_, _, _, _) => {
            socket setAlive uid
            member push pong
          }
        ) -> enum
      }
    }
}
