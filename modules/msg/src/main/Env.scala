package lila.msg

import com.softwaremill.macwire.*

import lila.common.Bus
import lila.common.Json.given
import lila.core.config.*
import lila.core.actorApi.socket.remote.TellUserIn

@Module
final class Env(
    baseUrl: BaseUrl,
    db: lila.db.Db,
    lightUserApi: lila.user.LightUserApi,
    getBotUserIds: lila.user.GetBotIds,
    isOnline: lila.core.socket.IsOnline,
    userRepo: lila.user.UserRepo,
    userCache: lila.user.Cached,
    relationApi: lila.core.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    notifyApi: lila.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    reportApi: lila.core.report.ReportApi,
    shutupApi: lila.core.shutup.ShutupApi,
    spam: lila.security.Spam,
    chatPanicAllowed: lila.core.chat.panic.IsAllowed,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor, akka.actor.ActorSystem, Scheduler, akka.stream.Materializer, lila.core.i18n.Translator):

  private val colls = wire[MsgColls]

  lazy val json = wire[MsgJson]

  private lazy val notifier = wire[MsgNotify]

  private lazy val security = wire[MsgSecurity]

  lazy val api: MsgApi = wire[MsgApi]

  lazy val search = wire[MsgSearch]

  lazy val compat = wire[MsgCompat]

  lazy val twoFactorReminder = wire[TwoFactorReminder]

  lazy val emailReminder = wire[EmailReminder]

  def cli: lila.common.Cli = new:
    def process =
      case "msg" :: "multi" :: orig :: dests :: words =>
        api.cliMultiPost(
          UserStr(orig),
          UserId.from(dests.map(_.toLower).split(',').toIndexedSeq),
          words.mkString(" ")
        )

  Bus.subscribeFuns(
    "msgSystemSend" -> { case lila.core.msg.SystemMsg(userId, text) =>
      api.systemPost(userId, text)
    },
    "remoteSocketIn:msgRead" -> { case TellUserIn(userId, msg) =>
      msg.get[UserId]("d").foreach { api.setRead(userId, _) }
    },
    "remoteSocketIn:msgSend" -> { case TellUserIn(userId, msg) =>
      for
        obj  <- msg.obj("d")
        dest <- obj.get[UserId]("dest")
        text <- obj.str("text")
      yield api.post(userId, dest, text)
    }
  )

  import play.api.data.Forms.*
  val textForm = play.api.data.Form(single("text" -> nonEmptyText))

private class MsgColls(db: lila.db.Db):
  val thread = db(CollName("msg_thread"))
  val msg    = db(CollName("msg_msg"))
