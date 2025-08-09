package lila.mailer

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration

import lila.common.Bus

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

  lazy val canSendEmailsSetting = settingStore[Boolean](
    "canSendEmails",
    default = true,
    text = "Lila can send emails. Toggle off when the email service is unavailable.".some
  ).taggedWith[CanSendEmails]

  lazy val mailerSecondaryPermilleSetting = settingStore[Int](
    "mailerSecondaryPermille",
    default = 0,
    text = "Permille of mails to send using secondary SMTP configuration".some
  )

  lazy val mailer = Mailer(
    config,
    canSendEmails = canSendEmailsSetting,
    getSecondaryPermille = () => mailerSecondaryPermilleSetting.get()
  )

  lazy val automaticEmail = wire[AutomaticEmail]

  Bus.sub[lila.core.fishnet.NewKey]:
    case lila.core.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key)

  Bus.sub[lila.core.plan.PlanStart]: plan =>
    automaticEmail.onPatronNew(plan.userId)

  Bus.sub[lila.core.plan.PlanGift]:
    case lila.core.plan.PlanGift(from, to, lifetime) =>
      automaticEmail.onPatronGift(from, to, lifetime)

  Bus.sub[lila.core.plan.PlanExpire]: plan =>
    automaticEmail.onPatronStop(plan.userId)

  Bus.sub[lila.core.misc.mailer.CorrespondenceOpponents]: game =>
    automaticEmail.dailyCorrespondenceNotice(game.userId, game.opponents)

trait CanSendEmails
