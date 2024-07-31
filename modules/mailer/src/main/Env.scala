package lila.mailer

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.Bus
import lila.common.config.*
import lila.core.misc.mailer.CorrespondenceOpponents

@Module
final class Env(
    appConfig: Configuration,
    net: lila.core.config.NetConfig,
    userApi: lila.core.user.UserApi,
    settingStore: lila.memo.SettingStore.Builder,
    lightUser: lila.core.user.LightUserApi
)(using Executor, ActorSystem, Scheduler, lila.core.i18n.Translator):
  private val baseUrl = net.baseUrl
  import Mailer.given

  private val config = appConfig.get[Mailer.Config]("mailer")

  lazy val mailerSecondaryPermilleSetting = settingStore[Int](
    "mailerSecondaryPermille",
    default = 0,
    text = "Permille of mails to send using secondary SMTP configuration".some
  )

  lazy val mailer = Mailer(
    config,
    getSecondaryPermille = () => mailerSecondaryPermilleSetting.get()
  )

  lazy val automaticEmail = wire[AutomaticEmail]

  Bus.subscribeFuns(
    "fishnet" -> { case lila.core.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key)
    },
    "planStart" -> {
      case lila.core.misc.plan.PlanStart(userId) =>
        automaticEmail.onPatronNew(userId)
      case lila.core.misc.plan.PlanGift(from, to, lifetime) =>
        automaticEmail.onPatronGift(from, to, lifetime)
    },
    "planExpire" -> { case lila.core.misc.plan.PlanExpire(userId) =>
      automaticEmail.onPatronStop(userId)
    }
  )
  Bus.sub[CorrespondenceOpponents]:
    case CorrespondenceOpponents(userId, opponents) =>
      automaticEmail.dailyCorrespondenceNotice(userId, opponents)
