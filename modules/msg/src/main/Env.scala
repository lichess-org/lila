package lila.msg

import com.softwaremill.macwire.*

import lila.common.Bus
import lila.common.config.*
import lila.common.Json.given
import lila.hub.actorApi.socket.remote.TellUserIn

@Module
final class Env(
    db: lila.db.Db,
    lightUserApi: lila.user.LightUserApi,
    getBotUserIds: lila.user.GetBotIds,
    isOnline: lila.socket.IsOnline,
    userRepo: lila.user.UserRepo,
    userCache: lila.user.Cached,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    notifyApi: lila.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    spam: lila.security.Spam,
    chatPanic: lila.chat.ChatPanic,
    shutup: lila.hub.actors.Shutup,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor, akka.actor.ActorSystem, Scheduler, akka.stream.Materializer):

  private val colls = wire[MsgColls]

  lazy val json = wire[MsgJson]

  private lazy val notifier = wire[MsgNotify]

  private lazy val security = wire[MsgSecurity]

  lazy val api: MsgApi = wire[MsgApi]

  lazy val search = wire[MsgSearch]

  lazy val compat = wire[MsgCompat]

  lazy val twoFactorReminder = wire[TwoFactorReminder]

  def cli: lila.common.Cli = new:
    def process =
      case "msg" :: "multi" :: orig :: dests :: words =>
        api.cliMultiPost(
          UserStr(orig),
          UserId.from(dests.map(_.toLower).split(',').toIndexedSeq),
          words mkString " "
        )

  Bus.subscribeFuns(
    "msgSystemSend" -> { case lila.hub.actorApi.msg.SystemMsg(userId, text) =>
      api.systemPost(userId, text)
    },
    "remoteSocketIn:msgRead" -> { case TellUserIn(userId, msg) =>
      msg.get[UserId]("d") foreach { api.setRead(userId, _) }
    },
    "remoteSocketIn:msgSend" -> { case TellUserIn(userId, msg) =>
      for
        obj  <- msg obj "d"
        dest <- obj.get[UserId]("dest")
        text <- obj str "text"
      yield api.post(userId, dest, text)
    }
  )

  import play.api.data.Forms.*
  val textForm = play.api.data.Form(single("text" -> nonEmptyText))

private class MsgColls(db: lila.db.Db):
  val thread = db(CollName("msg_thread"))
  val msg    = db(CollName("msg_msg"))
