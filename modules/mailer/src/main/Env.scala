package lila.mailer

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.*
import lila.user.UserRepo

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    userRepo: UserRepo,
    settingStore: lila.memo.SettingStore.Builder,
    lightUser: lila.user.LightUserApi
)(using Executor, ActorSystem, Scheduler):
  private val baseUrl = net.baseUrl
  import Mailer.given

  private val config = appConfig.get[Mailer.Config]("mailer")

  lazy val mailerSecondaryPermilleSetting = settingStore[Int](
    "mailerSecondaryPermille",
    default = 0,
    text = "Permille of mails to send using secondary SMTP configuration".some
  )

  lazy val mailer = new Mailer(
    config,
    getSecondaryPermille = () => mailerSecondaryPermilleSetting.get()
  )

  lazy val automaticEmail = wire[AutomaticEmail]

  lila.common.Bus.subscribeFuns(
    "fishnet" -> { case lila.hub.actorApi.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key).unit
    },
    "planStart" -> {
      case lila.hub.actorApi.plan.PlanStart(userId) =>
        automaticEmail.onPatronNew(userId).unit
      case lila.hub.actorApi.plan.PlanGift(from, to, lifetime) =>
        automaticEmail.onPatronGift(from, to, lifetime).unit
    },
    "planExpire" -> { case lila.hub.actorApi.plan.PlanExpire(userId) =>
      automaticEmail.onPatronStop(userId).unit
    },
    "dailyCorrespondenceNotif" -> {
      case lila.hub.actorApi.mailer.CorrespondenceOpponents(userId, opponents) =>
        automaticEmail.dailyCorrespondenceNotice(userId, opponents).unit
    }
  )
