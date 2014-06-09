package lila.pool

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._

import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.SendTos
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import lila.user.User

private[pool] final class PoolActor(
    setup: PoolSetup,
    val history: History,
    lightUser: String => Option[LightUser],
    isOnline: String => Boolean,
    renderer: ActorSelection,
    uidTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member] {

  private var pool = Pool(setup, Nil)
  lila.user.UserRepo randomDudes scala.util.Random.nextInt(500) foreach { users =>
    pool = users.foldLeft(pool)(_ withUser _)
  }

  // last time each user waved to the pool
  private val wavers = new lila.memo.ExpireSetMemo(10 seconds)

  def receiveSpecific = {

    case GetPool => sender ! pool

    case Enter(user) =>
      pool = pool withUser user
      wavers put user.id
      sender ! true

    case Wave(user) =>
      wavers put user.id

    case Leave(user) =>
      pool = pool withoutUser user
      sender ! true

    case Broom =>
      broom
      pool = pool filterPlayers { p =>
        wavers get p.user.id
      }
      pool.players map (_.user.id) filter isOnline foreach wavers.put

    case GetVersion => sender ! history.version

    // case StartGame(game) => game.players foreach { player =>
    //   for {
    //     userId ← player.userId
    //     member ← memberByUserId(userId)
    //   } {
    //     notifyMember("redirect", game fullIdOf player.color)(member)
    //     notifyReload
    //   }
    // }

    case Pairing =>
      import makeTimeout.short
      renderer ? RemindPool(pool) foreach {
        case html: play.twirl.api.Html =>
          val event = SendTos(pool.players.map(_.user.id).toSet, Json.obj(
            "t" -> "poolReminder",
            "d" -> Json.obj(
              "id" -> pool.setup.id,
              "html" -> html.toString
            )))
          context.system.lilaBus.publish(event, 'users)
      }

    case Reload => notifyReload

    case PingVersion(uid, v) =>
      ping(uid)
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersionTrollable("message", lila.chat.Line toJson line, troll = line.troll)
      case _ =>
    }

    // case GetVersion => sender ! history.version

    case Join(uid, user, version) =>
      import play.api.libs.iteratee._
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      sender ! Connected(enumerator, member)

    case Quit(uid) => {
      quit(uid)
      notifyCrowd
    }
  }

  def notifyCrowd {
    val (anons, users) = members.values.map(_.userId flatMap lightUser).foldLeft(0 -> List[LightUser]()) {
      case ((anons, users), Some(user)) => anons -> (user :: users)
      case ((anons, users), None)       => (anons + 1) -> users
    }
    notifyVersion("crowd", showSpectators(users, anons))
  }

  def notifyReload {
    notifyVersion("reload", JsNull)
  }

  def notifyVersionTrollable[A: Writes](t: String, data: A, troll: Boolean) {
    val vmsg = history += History.Message(makeMessage(t, data), troll)
    members.values.foreach(sendMessage(vmsg))
  }
}
