package lila.msg

import com.softwaremill.macwire._

import lila.common.Bus
import lila.common.config._
import lila.user.User
import lila.hub.actorApi.socket.remote.TellUserIn

@Module
final class Env(
    db: lila.db.Db,
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.socket.IsOnline,
    userRepo: lila.user.UserRepo,
    userCache: lila.user.Cached,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    notifyApi: lila.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    spam: lila.security.Spam,
    chatPanic: lila.chat.ChatPanic,
    shutup: lila.hub.actors.Shutup
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    scheduler: akka.actor.Scheduler
) {

  private val colls = wire[MsgColls]

  lazy val json = wire[MsgJson]

  private lazy val notifier = wire[MsgNotify]

  private lazy val security = wire[MsgSecurity]

  lazy val api: MsgApi = wire[MsgApi]

  lazy val search = wire[MsgSearch]

  lazy val compat = wire[MsgCompat]

  def cli =
    new lila.common.Cli {
      def process = { case "msg" :: "multi" :: orig :: dests :: words =>
        api.cliMultiPost(orig, dests.map(_.toLower).split(',').toIndexedSeq, words mkString " ")
      }
    }

  Bus.subscribeFuns(
    "msgSystemSend" -> { case lila.hub.actorApi.msg.SystemMsg(userId, text) =>
      api.systemPost(userId, text).unit
    },
    "remoteSocketIn:msgRead" -> { case TellUserIn(userId, msg) =>
      msg str "d" map User.normalize foreach { api.setRead(userId, _) }
    },
    "remoteSocketIn:msgSend" -> { case TellUserIn(userId, msg) =>
      for {
        obj  <- msg obj "d"
        dest <- obj str "dest" map User.normalize
        text <- obj str "text"
      } api.post(userId, dest, text)
    }
  )
}

private class MsgColls(db: lila.db.Db) {
  val thread = db(CollName("msg_thread"))
  val msg    = db(CollName("msg_msg"))
}
