package lila.msg

import com.softwaremill.macwire.*

import lila.common.Bus
import lila.common.Json.given
import lila.core.config.*
import lila.core.socket.remote.TellUserIn

@Module
final class Env(
    baseUrl: BaseUrl,
    db: lila.db.Db,
    lightUserApi: lila.core.user.LightUserApi,
    isOnline: lila.core.socket.IsOnline,
    userApi: lila.core.user.UserApi,
    userRepo: lila.core.user.UserRepo,
    userCache: lila.core.user.CachedApi,
    relationApi: lila.core.relation.RelationApi,
    prefApi: lila.core.pref.PrefApi,
    notifyApi: lila.core.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    reportApi: lila.core.report.ReportApi,
    shutupApi: lila.core.shutup.ShutupApi,
    spam: lila.core.security.SpamApi,
    chatPanicAllowed: lila.core.chat.panic.IsAllowed,
    textAnalyser: lila.core.shutup.TextAnalyser,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor, akka.actor.ActorSystem, Scheduler, akka.stream.Materializer, lila.core.i18n.Translator):

  private val colls = wire[MsgColls]

  private val contactApi = ContactApi(userRepo.coll)

  val json = wire[MsgJson]

  private val notifier = wire[MsgNotify]

  private val security = wire[MsgSecurity]

  val api: MsgApi = wire[MsgApi]

  val search = wire[MsgSearch]

  val compat = wire[MsgCompat]

  val twoFactorReminder = wire[TwoFactorReminder]

  val emailReminder = wire[EmailReminder]

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
