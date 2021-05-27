package lila.mailer

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.common.Strings
import lila.memo.SettingStore.Strings._
import lila.user.UserRepo

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    userRepo: UserRepo,
    settingStore: lila.memo.SettingStore.Builder
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    scheduler: Scheduler
) {
  import net.baseUrl

  private val config = appConfig.get[Mailer.Config]("mailer")(Mailer.configLoader)

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

  lila.common.Bus.subscribeFun("fishnet") { case lila.hub.actorApi.fishnet.NewKey(userId, key) =>
    automaticEmail.onFishnetKey(userId, key).unit
  }
}
