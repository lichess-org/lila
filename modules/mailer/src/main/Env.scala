package lila.mailer

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.*
import lila.user.{ UserApi, UserRepo }

@Module
final class Env(
    appConfig: Configuration,
    net: lila.core.config.NetConfig,
    userRepo: UserRepo,
    userApi: UserApi,
    settingStore: lila.memo.SettingStore.Builder,
    lightUser: lila.user.LightUserApi
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

  lila.common.Bus.subscribeFuns(
    "fishnet" -> { case lila.core.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key)
    },
    "planStart" -> {
      case lila.core.actorApi.plan.PlanStart(userId) =>
        automaticEmail.onPatronNew(userId)
      case lila.core.actorApi.plan.PlanGift(from, to, lifetime) =>
        automaticEmail.onPatronGift(from, to, lifetime)
    },
    "planExpire" -> { case lila.core.actorApi.plan.PlanExpire(userId) =>
      automaticEmail.onPatronStop(userId)
    },
    "dailyCorrespondenceNotif" -> {
      case lila.core.actorApi.mailer.CorrespondenceOpponents(userId, opponents) =>
        automaticEmail.dailyCorrespondenceNotice(userId, opponents)
    }
  )
